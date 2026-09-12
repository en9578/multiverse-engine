package com.minbao.multiverse.manager.impl;

import com.minbao.multiverse.collector.DataFreshnessService;
import com.minbao.multiverse.collector.domain.FreshnessInfo;
import com.minbao.multiverse.collector.domain.MarketDataCategory;
import com.minbao.multiverse.collector.kb.KbEntry;
import com.minbao.multiverse.collector.kb.KnowledgeBaseRegistry;
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
import java.time.LocalDate;
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
    /** 无市场事实时策略画像先验缺省值（0-100） */
    private static final double DEFAULT_PRIOR = 72.0;

    @Resource
    private UniverseRater universeRater;

    /** P3 KB 注册中心（启动加载 kb/*.yml），RULE_COMPLIANCE_KB 真读 policy_kb */
    @Resource
    private KnowledgeBaseRegistry kbRegistry;

    /** TTL 时效判定，KB 规则扣分按 freshness 权重（kb 1.0 / kb_stale 0.5 / missing 不入账）缩放 */
    @Resource
    private DataFreshnessService freshnessService;

    @Override
    public double strategyPrior(UniverseBO universe) {
        Map<String, Object> pkg = JsonUtil.parseObject(universe.getStrategyPackage());
        if (pkg == null) return DEFAULT_PRIOR;
        String pricing = str(pkg.get("pricingStrategy"));
        String sellingPoint = str(pkg.get("sellingPointStrategy"));
        String positioning = str(pkg.get("positioningStrategy"));
        double prior = switch (pricing + "|" + sellingPoint + "|" + positioning) {
            case "高端|功能型|品类头部" -> 78.0;      // 溢价品质，红海但品牌力强
            case "高端|差异化型|细分垂直" -> 86.0;    // 高净值细分，稀缺卖点壁垒高
            case "性价比|功能型|细分垂直" -> 80.0;    // 垂直刚需，成本可控
            case "性价比|情感型|品类头部" -> 66.0;    // 大众走量，拼内容与复购
            case "低价引流|差异化型|价格破坏者" -> 45.0; // 极低价，利润薄、被跟价风险大
            default -> DEFAULT_PRIOR;
        };
        log.debug("策略画像先验 universeId={} pricing={} sellingPoint={} positioning={} prior={}",
                universe.getUniverseId(), pricing, sellingPoint, positioning, prior);
        return prior;
    }

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
        score += applyKbPolicyRule(universe, evidences);

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
        String desc = (high > 0 || medium > 0)
                ? String.format("该市场有 %d 项高风险、%d 项中风险合规要求，进入前需先解决，否则会被下架或罚款", high, medium)
                : "未发现明显合规风险，此项不扣分";
        evidences.add(evidence("RULE_COMPLIANCE_RISK",
                "high=" + high + ", medium=" + medium, desc, deduction, 1.0));
        return deduction;
    }

    /** 竞争密度：竞品 <=3 不扣分，4-8 扣 10，>8 扣 20 */
    private double applyCompetitionRule(CollectedDataBO data, List<EvolutionResultBO.RuleEvidence> evidences) {
        int count = listOf(data.getCompetitorData(), "competitors").size();
        double deduction = count > 8 ? -20 : (count >= 4 ? -10 : 0);
        String density = count > 8 ? "竞争激烈，获客成本高" : (count >= 4 ? "竞争适中" : "竞争压力小");
        evidences.add(evidence("RULE_COMPETITION",
                "competitorCount=" + count,
                String.format("该市场现有 %d 个同类竞品，%s", count, density), deduction, 0.8));
        return deduction;
    }

    /** 评论情绪：sentiment <0.4 扣 20，<0.6 扣 10（缺失不扣分，input 标注缺省） */
    private double applyReviewRule(CollectedDataBO data, List<EvolutionResultBO.RuleEvidence> evidences) {
        boolean has = data.getReviewData() != null && data.getReviewData().get("sentiment") instanceof Number;
        double s = has ? ((Number) data.getReviewData().get("sentiment")).doubleValue() : 0.7;
        double deduction = s < 0.4 ? -20 : (s < 0.6 ? -10 : 0);
        String input = has ? String.format("sentiment=%.2f", s) : "sentiment=缺失(缺省0.7,不扣分)";
        String mood = s < 0.4 ? "整体偏负面，差评风险高" : (s < 0.6 ? "好坏参半" : "整体偏正面");
        String desc = has ? String.format("买家评论情绪得分 %.2f（满分 1 分），%s", s, mood)
                : "暂未获取到买家评论数据，按经验缺省值处理，此项不扣分";
        evidences.add(evidence("RULE_REVIEW_SENTIMENT", input, desc, deduction, 0.9));
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
            evidences.add(evidence("RULE_PRICE_POSITION", "no price info",
                    "暂无定价与竞品价格数据，此项不扣分", 0, 0.5));
            return 0;
        }
        double premium = (myPrice - avg) / avg;
        double deduction = premium > 0.5 ? -15 : 0;
        String posn = premium > 0.5
                ? String.format("高出市场 %.0f%%，溢价过高，可能流失价格敏感买家", premium * 100)
                : premium > 0
                        ? String.format("高出市场 %.0f%%，溢价在合理范围", premium * 100)
                        : "低于市场均价，价格更有吸引力";
        evidences.add(evidence("RULE_PRICE_POSITION",
                String.format("myPrice=%.2f, avgCompetitorPrice=%.2f, premium=%.0f%%",
                        myPrice, avg, premium * 100),
                String.format("本策略定价 %.0f 元（竞品均价 %.0f 元），%s", myPrice, avg, posn),
                deduction, 0.7));
        return deduction;
    }

    /** KB 政策合规基线（source=kb/kb_stale，真读 policy_kb）：区域匹配条目按 severity×时效权重扣分，evidence 引用 KB 条目 id。
     *  政策条目无产品品类字段，故为市场级注册/合规负担基线，品类关键词过滤留后续（见 CLAUDE.md 已知偏差）。 */
    private double applyKbPolicyRule(UniverseBO universe, List<EvolutionResultBO.RuleEvidence> evidences) {
        String market = universe.getTargetMarket();
        if (market == null || market.isBlank()) {
            return 0;
        }
        List<KbEntry> policies = kbRegistry.findByCategoryAndMarket(MarketDataCategory.POLICY, market);
        if (policies.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        double deduction = 0;
        for (KbEntry e : policies) {
            int ttl = e.getFreshnessTtlDays() == null ? 90 : e.getFreshnessTtlDays();
            FreshnessInfo fi = freshnessService.evaluate(e.getLastVerified(), ttl, today);
            if (fi.weight() <= 0) {
                continue; // MISSING（last_verified 缺失）：不入账不扣分，避免编造
            }
            double base = switch (e.getSeverity() == null ? "" : e.getSeverity().toLowerCase()) {
                case "high" -> 8;
                case "medium" -> 4;
                case "low" -> 2;
                default -> 0;
            };
            if (base == 0) {
                continue;
            }
            double contrib = -(base * fi.weight());
            deduction += contrib;
            String input = String.format("KB政策=%s(%s) market=%s->%s severity=%s freshness=%s(weight=%.1f)",
                    e.getId(), e.getName(),
                    e.getMarket() == null ? "ALL" : e.getMarket(),
                    market, e.getSeverity(), fi.status().name(), fi.weight());
            boolean stale = "kb_stale".equals(fi.source());
            String tail = stale
                    ? String.format("；该政策数据已过保鲜期，扣分按半价计为 %.1f 分", -contrib)
                    : String.format("，扣 %.0f 分", -contrib);
            String desc = String.format("进入该市场需完成《%s》相关合规注册（%s）%s",
                    e.getName(), severityText(e.getSeverity()), tail);
            evidences.add(evidence("RULE_COMPLIANCE_KB", input, desc,
                    Math.round(contrib * 10) / 10.0, fi.weight(), fi.source()));
        }
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

    /** 规则中文名映射（ruleId → 面向业务用户的人话标题） */
    private static String labelFor(String ruleId) {
        return switch (ruleId) {
            case "RULE_COMPLIANCE_RISK" -> "合规风险";
            case "RULE_COMPETITION" -> "竞争密度";
            case "RULE_REVIEW_SENTIMENT" -> "评论情绪";
            case "RULE_PRICE_POSITION" -> "价格定位";
            case "RULE_COMPLIANCE_KB" -> "政策合规";
            case "RULE_DEGRADED_PRIOR" -> "演示模式评分";
            case "R1_INFERRED" -> "AI 推演结论";
            default -> ruleId;
        };
    }

    private static String severityText(String s) {
        return switch (s == null ? "" : s.toLowerCase()) {
            case "high" -> "高风险";
            case "medium" -> "中风险";
            case "low" -> "低风险";
            default -> "合规要求";
        };
    }

    /** 启发式规则证据（默认 source=heuristic：确定性规则/无 KB 依据，不再冒充 kb） */
    private EvolutionResultBO.RuleEvidence evidence(String ruleId, String input, String description, double output, double weight) {
        return evidence(ruleId, input, description, output, weight, EvolutionResultBO.RuleEvidence.SRC_HEURISTIC);
    }

    private EvolutionResultBO.RuleEvidence evidence(String ruleId, String input, String description, double output, double weight, String source) {
        EvolutionResultBO.RuleEvidence e = new EvolutionResultBO.RuleEvidence();
        e.setRuleId(ruleId);
        e.setLabel(labelFor(ruleId));
        e.setDescription(description);
        e.setInput(input);
        e.setOutput(String.valueOf(output));
        e.setWeight(weight);
        e.setSource(source);
        return e;
    }
}
