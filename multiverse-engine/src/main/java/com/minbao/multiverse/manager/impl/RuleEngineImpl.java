package com.minbao.multiverse.manager.impl;

import com.minbao.multiverse.common.JsonUtil;
import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.EvolutionResultBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.engine.rating.UniverseRater;
import com.minbao.multiverse.enums.UniverseRatingEnum;
import com.minbao.multiverse.manager.RuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 规则引擎：可解释推演。
 * 每条规则输出一条 RuleEvidence（ruleId / input / output / weight），推演结果可逐条追溯。
 * 规则分从 100 起扣，扣完得 score，survivalRate = score / 100，评级走 UniverseRater。
 */
@Service
public class RuleEngineImpl implements RuleEngine {
    private static final Logger log = LoggerFactory.getLogger(RuleEngineImpl.class);

    private static final double BASE_SCORE = 100.0;

    @Resource
    private UniverseRater universeRater;

    @Override
    public EvolutionResultBO evolve(UniverseBO universe, CollectedDataBO data) {
        log.info("规则引擎推演 universeId={} index={}", universe.getUniverseId(), universe.getUniverseIndex());
        EvolutionResultBO result = new EvolutionResultBO();
        List<EvolutionResultBO.RuleEvidence> evidences = new ArrayList<>();
        double score = BASE_SCORE;

        score += applyComplianceRule(data, evidences);
        score += applyCompetitionRule(data, evidences);
        score += applyReviewRule(data, evidences);
        score += applyPriceRule(universe, data, evidences);

        score = Math.max(5, Math.min(99, score));
        result.setSurvivalRate(Math.round(score) / 100.0);
        UniverseRatingEnum rating = universeRater.rate(score);
        result.setRating(rating.name());
        result.setEvidences(evidences);
        log.info("规则推演完成 universeId={} score={} survivalRate={} rating={}",
                universe.getUniverseId(), score, result.getSurvivalRate(), rating);
        return result;
    }

    /** 合规风险：high 每条 -15，medium 每条 -5 */
    private double applyComplianceRule(CollectedDataBO data, List<EvolutionResultBO.RuleEvidence> evidences) {
        int high = 0, medium = 0;
        for (Map<String, Object> item : listOf(data.getComplianceData(), "compliance")) {
            String level = str(item.get("level"));
            if ("high".equalsIgnoreCase(level)) high++;
            else if ("medium".equalsIgnoreCase(level)) medium++;
        }
        double deduction = -(high * 15 + medium * 5);
        evidences.add(evidence("RULE_COMPLIANCE_RISK",
                "high=" + high + ", medium=" + medium, deduction, 1.0));
        return deduction;
    }

    /** 竞争密度：竞品 <=3 不扣分，4-8 扣 10，>8 扣 20 */
    private double applyCompetitionRule(CollectedDataBO data, List<EvolutionResultBO.RuleEvidence> evidences) {
        int count = listOf(data.getCompetitorData(), "competitors").size();
        double deduction = count > 8 ? -20 : (count >= 4 ? -10 : 0);
        evidences.add(evidence("RULE_COMPETITION",
                "competitorCount=" + count, deduction, 0.8));
        return deduction;
    }

    /** 评论情绪：sentiment <0.4 扣 20，<0.6 扣 10 */
    private double applyReviewRule(CollectedDataBO data, List<EvolutionResultBO.RuleEvidence> evidences) {
        Object sentiment = data.getReviewData() != null ? data.getReviewData().get("sentiment") : null;
        double s = sentiment instanceof Number ? ((Number) sentiment).doubleValue() : 0.7;
        double deduction = s < 0.4 ? -20 : (s < 0.6 ? -10 : 0);
        evidences.add(evidence("RULE_REVIEW_SENTIMENT",
                "sentiment=" + s, deduction, 0.9));
        return deduction;
    }

    /** 价格定位：策略定价高于竞品均价 50% 以上扣 15（溢价过高），无定价信息不扣分 */
    private double applyPriceRule(UniverseBO universe, CollectedDataBO data,
                                  List<EvolutionResultBO.RuleEvidence> evidences) {
        Double myPrice = extractPrice(universe.getStrategyPackage());
        List<Map<String, Object>> competitors = listOf(data.getCompetitorData(), "competitors");
        double avg = competitors.stream()
                .map(c -> c.get("price"))
                .filter(p -> p instanceof Number)
                .mapToDouble(p -> ((Number) p).doubleValue())
                .average().orElse(0);
        if (myPrice == null || avg <= 0) {
            evidences.add(evidence("RULE_PRICE_POSITION", "no price info", 0, 0.5));
            return 0;
        }
        double premium = (myPrice - avg) / avg;
        double deduction = premium > 0.5 ? -15 : 0;
        evidences.add(evidence("RULE_PRICE_POSITION",
                String.format("myPrice=%.2f, avgCompetitorPrice=%.2f, premium=%.0f%%",
                        myPrice, avg, premium * 100),
                deduction, 0.7));
        return deduction;
    }

    /** 从策略包 JSON 中尽力提取 price 字段（策略包由 LLM 生成，字段可能缺失） */
    private Double extractPrice(String strategyPackage) {
        Map<String, Object> pkg = JsonUtil.parseObject(strategyPackage);
        if (pkg == null) return null;
        Object price = pkg.get("price");
        if (price instanceof Number) return ((Number) price).doubleValue();
        if (price instanceof String) {
            try { return Double.parseDouble(((String) price).replaceAll("[^0-9.]", "")); }
            catch (NumberFormatException ignored) { }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOf(Map<String, Object> data, String key) {
        if (data == null) return List.of();
        Object v = data.get(key);
        return v instanceof List ? (List<Map<String, Object>>) v : List.of();
    }

    private String str(Object v) { return v == null ? "" : v.toString(); }

    private EvolutionResultBO.RuleEvidence evidence(String ruleId, String input, double output, double weight) {
        EvolutionResultBO.RuleEvidence e = new EvolutionResultBO.RuleEvidence();
        e.setRuleId(ruleId);
        e.setInput(input);
        e.setOutput(String.valueOf(output));
        e.setWeight(weight);
        e.setSource("kb");
        return e;
    }
}
