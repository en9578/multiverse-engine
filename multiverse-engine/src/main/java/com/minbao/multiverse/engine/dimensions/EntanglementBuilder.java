package com.minbao.multiverse.engine.dimensions;

import com.minbao.multiverse.common.JsonUtil;
import com.minbao.multiverse.dao.CompetitorReactionDAO;
import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.EvolutionResultBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.domain.entity.CompetitorReactionDO;
import com.minbao.multiverse.engine.evolution.R1Enhancer;
import com.minbao.multiverse.enums.ReactionTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 关联维度宇宙（设计 §3.3.3 / §4.2）。
 * 推演竞品对卖家策略的反应（跟价/跟款/差异化/无视）：
 * 规则基线做确定性启发式推演（source=heuristic，按竞品价格/评分/销量规模阈值判断）+ R1 推理增强补充
 * （source=r1_inferred，规则未覆盖场景）。competitor_strategy_kb 反哺规则后 rule 行方可升为 source=kb。
 */
@Component
public class EntanglementBuilder {
    private static final Logger log = LoggerFactory.getLogger(EntanglementBuilder.class);

    @Resource private CompetitorReactionDAO competitorReactionDAO;
    @Resource private R1Enhancer r1Enhancer;

    public void build(UniverseBO universe, CollectedDataBO data, String traceId) {
        Map<String, Object> pkg = JsonUtil.parseObject(universe.getStrategyPackage());
        double ourPrice = number(pkg == null ? null : pkg.get("price"));

        // 1. 规则基线（可解释）
        int ruleCount = 0;
        for (Map<String, Object> comp : listOf(data.getCompetitorData(), "competitors")) {
            competitorReactionDAO.insert(ruleReaction(universe.getUniverseId(), ourPrice, comp, traceId));
            ruleCount++;
        }

        // 2. R1 增强（规则未覆盖场景，source=r1_inferred）
        String r1 = r1Enhancer.enhance(systemPrompt(), userPrompt(universe, data));
        Map<String, Object> parsed = JsonUtil.parseObject(r1);
        int r1Count = 0;
        if (parsed != null && parsed.get("reactions") instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> m)) continue;
                CompetitorReactionDO reaction = new CompetitorReactionDO();
                reaction.setUniverseId(universe.getUniverseId());
                reaction.setCompetitorName(str(m.get("competitorName"), "未知竞品"));
                reaction.setReactionType(str(m.get("reactionType"), ReactionTypeEnum.DIFFERENTIATE.getLabel()));
                reaction.setProbability(clamp(number(m.get("probability")) == 0 ? 0.5 : number(m.get("probability"))));
                reaction.setImpact(str(m.get("impact"), ""));
                reaction.setSource("r1_inferred");
                reaction.setEvidence(str(m.get("evidence"), ""));
                reaction.setTraceId(traceId);
                competitorReactionDAO.insert(reaction);
                r1Count++;
            }
        }
        log.info("关联推演完成 universeId={} ruleReactions={} r1Reactions={}",
                universe.getUniverseId(), ruleCount, r1Count);
    }

    /** 规则基线：按竞品价格差/评分/销量规模匹配反应模式 */
    private CompetitorReactionDO ruleReaction(Long universeId, double ourPrice,
                                             Map<String, Object> comp, String traceId) {
        String name = str(comp.get("name"), "竞品");
        double compPrice = number(comp.get("price"));
        double compRating = number(comp.get("rating"));
        double compReview = number(comp.get("reviewCount"));

        ReactionTypeEnum type;
        double prob;
        String impact;
        String evidence;
        if (ourPrice > 0 && compPrice > 0 && ourPrice < compPrice * 0.9) {
            type = ReactionTypeEnum.FOLLOW_PRICE;
            prob = 0.75;
            impact = "价格战风险";
            evidence = "我方定价显著低于竞品，竞品大概率跟价";
        } else if (compRating >= 4.5) {
            type = ReactionTypeEnum.FOLLOW_PRODUCT;
            prob = 0.60;
            impact = "卖点同质化";
            evidence = "竞品评分高，倾向跟进我方爆款卖点";
        } else if (compReview > 0 && compReview < 50) {
            type = ReactionTypeEnum.IGNORE;
            prob = 0.55;
            impact = "无实质影响";
            evidence = "竞品销量规模小，反应意愿弱";
        } else {
            type = ReactionTypeEnum.DIFFERENTIATE;
            prob = 0.50;
            impact = "分流竞争";
            evidence = "竞品错位竞争，走差异化";
        }

        CompetitorReactionDO reaction = new CompetitorReactionDO();
        reaction.setUniverseId(universeId);
        reaction.setCompetitorName(name);
        reaction.setReactionType(type.getLabel());
        reaction.setProbability(clamp(prob));
        reaction.setImpact(impact);
        reaction.setSource(EvolutionResultBO.RuleEvidence.SRC_HEURISTIC);
        reaction.setEvidence(evidence);
        reaction.setTraceId(traceId);
        return reaction;
    }

    private String systemPrompt() {
        return """
                你是竞品关联推演官（场景五方向②）。基于该宇宙策略与市场事实，识别规则引擎可能未覆盖的竞品关联反应。
                只输出严格合法的 JSON，不要输出任何解释文字或 markdown 标记。JSON 结构：
                {"reactions":[{"competitorName":"竞品名","reactionType":"跟价|跟款|差异化|无视","probability":0.6,"impact":"影响一句话","evidence":"推理依据一句话"}]}
                要求：只补充规则未覆盖的场景，reactions 可空数组。""";
    }

    private String userPrompt(UniverseBO universe, CollectedDataBO data) {
        return String.format("""
                该宇宙策略包：%s
                市场竞品事实：%s
                请补充规则未覆盖的竞品关联反应 JSON。""",
                universe.getStrategyPackage(),
                JsonUtil.toJson(Map.of("competitors", data.getCompetitorData().get("competitors"))));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOf(Map<String, Object> data, String key) {
        if (data == null) return List.of();
        Object v = data.get(key);
        return v instanceof List ? (List<Map<String, Object>>) v : List.of();
    }

    private double number(Object v) {
        return v instanceof Number ? ((Number) v).doubleValue() : 0;
    }

    private String str(Object v, String def) { return v == null || v.toString().isBlank() ? def : v.toString(); }

    private double clamp(double v) { return Math.max(0.05, Math.min(0.99, v)); }
}
