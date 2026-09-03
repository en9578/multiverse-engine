package com.minbao.multiverse.manager.impl;

import com.minbao.multiverse.common.BusinessException;
import com.minbao.multiverse.common.JsonUtil;
import com.minbao.multiverse.collector.DataCollector;
import com.minbao.multiverse.dao.SettlementDecisionDAO;
import com.minbao.multiverse.dao.StressTestDAO;
import com.minbao.multiverse.dao.UniverseDAO;
import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.EvolutionResultBO;
import com.minbao.multiverse.domain.bo.SettlementBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.domain.entity.MultiverseTaskDO;
import com.minbao.multiverse.domain.entity.SettlementDecisionDO;
import com.minbao.multiverse.domain.entity.StressTestDO;
import com.minbao.multiverse.domain.entity.UniverseDO;
import com.minbao.multiverse.engine.MultiverseGenerator;
import com.minbao.multiverse.engine.rating.UniverseRater;
import com.minbao.multiverse.enums.DimensionEnum;
import com.minbao.multiverse.enums.ErrorCodeEnum;
import com.minbao.multiverse.enums.StageEnum;
import com.minbao.multiverse.enums.UniverseRatingEnum;
import com.minbao.multiverse.manager.BailianManager;
import com.minbao.multiverse.manager.MultiverseEngine;
import com.minbao.multiverse.manager.RuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 多元宇宙引擎实现（四阶段：collect → generate → explore → settle）。
 * 生成阶段委托 {@link MultiverseGenerator}：3 时间宇宙 + 5 策略宇宙（含 关联/极端/天气 三维度）。
 * 推演阶段：规则基线 + LLM 推演 0.7/0.3 融合评级，极端维度整体存活率作为推演输入。
 * LLM 输出解析失败时逐级降级（模板宇宙 / 仅规则推演），不中断整体编排。
 */
@Service
public class MultiverseEngineImpl implements MultiverseEngine {
    private static final Logger log = LoggerFactory.getLogger(MultiverseEngineImpl.class);

    /** LLM 推演分与规则基线分的融合权重 */
    private static final double LLM_WEIGHT = 0.7;
    private static final double RULE_WEIGHT = 0.3;

    @Resource private UniverseDAO universeDAO;
    @Resource private StressTestDAO stressTestDAO;
    @Resource private SettlementDecisionDAO settlementDecisionDAO;
    @Resource private BailianManager bailianManager;
    @Resource private RuleEngine ruleEngine;
    @Resource private UniverseRater universeRater;
    @Resource private MultiverseGenerator multiverseGenerator;
    @Resource private DataCollector dataCollector;
    @Resource @Qualifier("bailianExecutor") private ThreadPoolTaskExecutor bailianExecutor;

    // ==================== 阶段一：数据采集 ====================

    @Override
    public CollectedDataBO collectData(MultiverseTaskDO task) {
        log.info("收集数据 taskId={} productName={} targetMarket={}",
                task.getId(), task.getProductName(), task.getTargetMarket());

        String systemPrompt = """
                你是跨境电商市场数据分析师。基于产品信息给出目标市场的结构化事实数据。
                只输出严格合法的 JSON，不要输出任何解释文字或 markdown 标记。JSON 结构：
                {"competitors":[{"name":"竞品名","price":29.9,"rating":4.5,"reviewCount":1200,"sellerCount":8}],
                 "compliance":[{"risk":"风险名","level":"high|medium|low","detail":"一句话说明"}],
                 "reviews":{"sentiment":0.62,"topComplaints":["投诉点1","投诉点2"],
                            "defects":[{"name":"缺陷名","frequency":"high|medium|low","severity":"critical|major|minor","solution":"改进建议"}]}}
                要求：competitors 给 3-8 个；compliance 给 2-4 条；sentiment 为 0-1 的差评率反向指标（越高越正面）。""";

        // P3：先采集真实数据源（frankfurter 汇率 + KB 三类 + Tavily 降级），已落库 market_data，不依赖 LLM
        CollectedDataBO data = dataCollector.collect(task);

        String userPrompt = String.format("""
                产品：%s
                目标市场：%s
                卖家策略描述：%s
                实时数据源参考（请基于这些事实判断，标注来源）：
                汇率：%s
                知识库：%s
                请输出该产品在此市场的 JSON 事实数据。""",
                task.getProductName(), task.getTargetMarket(),
                task.getStrategyDesc() == null ? "无" : task.getStrategyDesc(),
                JsonUtil.toJson(data.getExchangeRateData()),
                JsonUtil.toJson(data.getKnowledgeBaseData()));

        Map<String, Object> parsed = null;
        try {
            for (int i = 1; i <= 2; i++) {
                String raw = bailianManager.generateText(StageEnum.COLLECTING, systemPrompt, userPrompt);
                parsed = JsonUtil.parseObject(raw);
                if (parsed != null) break;
                log.warn("采集数据 JSON 解析失败，重试 taskId={} attempt={}", task.getId(), i);
            }
        } catch (Exception e) {
            log.warn("采集 LLM 调用失败，降级为真实数据源 + KB taskId={}", task.getId(), e);
        }

        if (parsed != null) {
            data.setCompetitorData(Map.of("competitors",
                    parsed.get("competitors") instanceof List ? parsed.get("competitors") : List.of()));
            data.setComplianceData(Map.of("compliance",
                    parsed.get("compliance") instanceof List ? parsed.get("compliance") : List.of()));
            data.setReviewData(parsed.get("reviews") instanceof Map ? (Map<String, Object>) parsed.get("reviews") : Map.of());
        } else {
            data.setCompetitorData(Map.of("competitors", List.of()));
            data.setComplianceData(Map.of("compliance", List.of()));
            data.setReviewData(Map.of());
            log.warn("采集 LLM 输出不可用（RULE_ONLY_FALLBACK）taskId={}", task.getId());
        }
        log.info("收集数据完成 taskId={} competitors={} complianceRisks={}",
                task.getId(),
                ((List<?>) data.getCompetitorData().get("competitors")).size(),
                ((List<?>) data.getComplianceData().get("compliance")).size());
        return data;
    }

    // ==================== 阶段二：多元宇宙生成 ====================

    @Override
    public List<UniverseBO> generateUniverses(MultiverseTaskDO task, CollectedDataBO data) {
        MultiverseGenerator.Multiverse multiverse = multiverseGenerator.generate(task, data);
        List<UniverseBO> all = new ArrayList<>(multiverse.timeUniverses());
        all.addAll(multiverse.strategyUniverses());
        log.info("生成宇宙完成 taskId={} time={} strategies={} total={}",
                task.getId(), multiverse.timeUniverses().size(), multiverse.strategyUniverses().size(), all.size());
        return all;
    }

    // ==================== 阶段三：并行推演 + 评级 ====================

    @Override
    public void exploreUniverses(MultiverseTaskDO task, CollectedDataBO data) {
        log.info("探索宇宙 taskId={}", task.getId());
        List<UniverseDO> universes = universeDAO.selectByTaskIdAndDimension(
                task.getId(), DimensionEnum.STRATEGY.getCode());
        if (universes.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.UNIVERSE_NOT_FOUND);
        }

        String systemPrompt = """
                你是多元宇宙推演官。基于市场事实、该宇宙策略包与极端压力测试结果，推演其 90 天生存格局。
                只输出严格合法的 JSON，不要输出任何解释文字或 markdown 标记。JSON 结构：
                {"score":78,"survivalRate":0.78,"reasoning":"推演结论（80字内，需引用事实依据）"}
                score 为 0-100 的生存分；必须结合合规风险、竞争密度、评论情绪、策略定位与压力测试整体存活率综合判断。""";

        List<CompletableFuture<Void>> futures = universes.stream()
                .map(u -> CompletableFuture.runAsync(
                        () -> exploreOneUniverse(task, data, u, systemPrompt), bailianExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("探索宇宙完成 taskId={} count={}", task.getId(), universes.size());
    }

    /** 推演单个宇宙：规则基线 + LLM 推演融合，解析失败降级为仅规则推演 */
    private void exploreOneUniverse(MultiverseTaskDO task, CollectedDataBO data,
                                    UniverseDO universe, String systemPrompt) {
        // 1. 规则基线（可解释 evidences）
        EvolutionResultBO ruleResult = ruleEngine.evolve(toUniverseBO(universe), data);
        double ruleScore = ruleResult.getSurvivalRate() * 100;

        // 2. 极端维度整体存活率（5 风暴均值），作为 LLM 推演输入
        double overallSurvival = queryOverallSurvival(universe.getId());

        // 3. LLM 推演
        double llmScore = Double.NaN;
        String reasoning = "";
        try {
            String userPrompt = String.format("""
                    产品：%s（目标市场：%s）
                    市场事实：%s
                    该宇宙策略包：%s
                    规则引擎基线评估：%s
                    极端压力测试整体存活率：%.2f
                    请推演该宇宙 90 天后的生存格局，输出 JSON。""",
                    task.getProductName(), task.getTargetMarket(),
                    JsonUtil.toJson(Map.of("competitors", data.getCompetitorData().get("competitors"),
                            "compliance", data.getComplianceData().get("compliance"),
                            "reviews", data.getReviewData())),
                    universe.getStrategyPackage(),
                    JsonUtil.toJson(Map.of("survivalRate", ruleResult.getSurvivalRate(),
                            "evidences", ruleResult.getEvidences())),
                    overallSurvival);

            String raw = bailianManager.generateText(StageEnum.EXPLORING, systemPrompt, userPrompt);
            Map<String, Object> parsed = JsonUtil.parseObject(raw);
            if (parsed != null && parsed.get("score") instanceof Number) {
                llmScore = ((Number) parsed.get("score")).doubleValue();
                reasoning = String.valueOf(parsed.getOrDefault("reasoning", ""));
            }
        } catch (Exception e) {
            log.warn("宇宙推演 LLM 调用失败，降级为仅规则推演 universeId={}", universe.getId(), e);
        }

        // 4. 融合：LLM 0.7 / 规则 0.3；LLM 缺失则用「策略画像先验 + 5 风暴压力」规则融合，撑起 5 宇宙区分度
        double finalScore;
        if (Double.isNaN(llmScore)) {
            double prior = ruleEngine.strategyPrior(toUniverseBO(universe));   // 策略画像先验 0-100
            double avgSurvival = overallSurvival * 100;                         // 5 风暴均值分
            double minSurvival = queryMinStormSurvival(universe.getId()) * 100; // 5 风暴最低分(风险)
            ruleScore = clampScore(0.50 * prior + 0.30 * avgSurvival + 0.20 * minSurvival);
            finalScore = ruleScore;
            reasoning = "无 LLM key：按「策略画像先验 + 5 风暴压力融合」兜底评分（市场事实缺失，规则无法扣分）";
            log.warn("宇宙推演 LLM 输出不可用（RULE_ONLY_FALLBACK + 画像先验）universeId={} finalScore={} prior={}",
                    universe.getId(), Math.round(finalScore), Math.round(prior));
            appendDegradedEvidence(ruleResult, prior, avgSurvival, minSurvival, finalScore);
        } else {
            finalScore = LLM_WEIGHT * llmScore + RULE_WEIGHT * ruleScore;
        }
        double survivalRate = Math.max(0.05, Math.min(0.99, finalScore / 100.0));
        UniverseRatingEnum rating = universeRater.rate(finalScore);

        // 5. 演化数据（含 evidences 可追溯链 + 极端维度整体存活率）+ 落库
        Map<String, Object> evolution = new java.util.HashMap<>();
        evolution.put("finalScore", Math.round(finalScore * 10) / 10.0);
        evolution.put("llmScore", Double.isNaN(llmScore) ? null : Math.round(llmScore * 10) / 10.0);
        evolution.put("ruleScore", Math.round(ruleScore * 10) / 10.0);
        evolution.put("survivalRate", survivalRate);
        evolution.put("overallSurvival", overallSurvival);
        evolution.put("reasoning", reasoning);
        evolution.put("evidences", buildEvidences(ruleResult, llmScore, reasoning));

        universeDAO.updateEvolution(universe.getId(), rating.name(), "EVOLVED",
                BigDecimal.valueOf(survivalRate).setScale(2, RoundingMode.HALF_UP),
                JsonUtil.toJson(evolution));
        log.info("宇宙推演完成 universeId={} finalScore={} rating={} llmUsed={} overallSurvival={}",
                universe.getId(), Math.round(finalScore), rating, !Double.isNaN(llmScore), overallSurvival);
    }

    /** 查询该策略宇宙 5 风暴压力测试的整体存活率（均值，无则 1.0） */
    private double queryOverallSurvival(Long universeId) {
        List<StressTestDO> tests = stressTestDAO.selectByUniverseId(universeId);
        if (tests == null || tests.isEmpty()) return 1.0;
        double avg = tests.stream()
                .mapToDouble(t -> t.getSurvivalRate() == null ? 1.0 : t.getSurvivalRate())
                .average().orElse(1.0);
        return Math.round(avg * 100) / 100.0;
    }

    /** 该策略宇宙 5 风暴中的最低存活率（最脆弱的极端场景，无则 1.0） */
    private double queryMinStormSurvival(Long universeId) {
        List<StressTestDO> tests = stressTestDAO.selectByUniverseId(universeId);
        if (tests == null || tests.isEmpty()) return 1.0;
        return tests.stream()
                .mapToDouble(t -> t.getSurvivalRate() == null ? 1.0 : t.getSurvivalRate())
                .min().orElse(1.0);
    }

    /** 降级融合分限幅（0-100，避免极端值压垮评级） */
    private double clampScore(double v) {
        return Math.max(5, Math.min(95, v));
    }

    /** 降级分支追加一条可解释证据：说明 finalScore 来自画像先验 + 风暴压力融合，非凭空数字 */
    private void appendDegradedEvidence(EvolutionResultBO ruleResult,
                                        double prior, double avgSurvival, double minSurvival, double score) {
        if (ruleResult.getEvidences() == null) return;
        EvolutionResultBO.RuleEvidence e = new EvolutionResultBO.RuleEvidence();
        e.setRuleId("RULE_DEGRADED_PRIOR");
        e.setInput(String.format("LLM 不可用，策略画像先验=%.1f, avgStormSurvival=%.1f, minStormSurvival=%.1f",
                prior, avgSurvival, minSurvival));
        e.setOutput(String.format("%.1f", score));
        e.setWeight(0.5);
        e.setSource("kb");
        ruleResult.getEvidences().add(e);
    }

    // ==================== 阶段四：结算决策 ====================

    @Override
    public SettlementBO settle(MultiverseTaskDO task) {
        log.info("结算 taskId={}", task.getId());
        List<UniverseDO> universes = universeDAO.selectByTaskIdAndDimension(
                task.getId(), DimensionEnum.STRATEGY.getCode());
        if (universes.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.UNIVERSE_NOT_FOUND);
        }

        // 本地兜底：按评级 + 存活率排序
        UniverseDO best = universes.stream()
                .sorted((a, b) -> {
                    int byRating = ratingRank(b.getRating()) - ratingRank(a.getRating());
                    if (byRating != 0) return byRating;
                    return Double.compare(
                            b.getSurvivalRate() == null ? 0 : b.getSurvivalRate().doubleValue(),
                            a.getSurvivalRate() == null ? 0 : a.getSurvivalRate().doubleValue());
                })
                .findFirst().orElseThrow();

        // LLM 汇总决策
        UniverseDO selected = best;
        String rationale = "规则兜底：评级与存活率综合最优";
        List<String> antiFragilePortfolio = new ArrayList<>();
        Double expectedProfit = null;
        Double confidence = 0.5;
        try {
            String systemPrompt = """
                    你是定居决策官。综合 5 个平行宇宙的推演结果，选出最优宇宙并给出反脆弱组合。
                    只输出严格合法的 JSON，不要输出任何解释文字或 markdown 标记。JSON 结构：
                    {"selectedUniverseIndex":2,"rationale":"选择理由（60字内）",
                     "antiFragilePortfolio":["组合动作1","组合动作2","组合动作3"],
                     "expectedProfit":1.2,"confidence":0.8}
                    selectedUniverseIndex 为选中宇宙的 universeIndex（1-5）；
                    expectedProfit 为单件预期利润（美元数字）；confidence 为 0-1 置信度。""";
            String universeSummaries = universes.stream()
                    .map(u -> String.format("#%d rating=%s survivalRate=%s strategy=%s",
                            u.getUniverseIndex(), u.getRating(),
                            u.getSurvivalRate() == null ? "?" : u.getSurvivalRate(),
                            u.getStrategyPackage()))
                    .collect(Collectors.joining("\n"));
            String userPrompt = String.format("产品：%s（目标市场：%s）\n各宇宙推演结果：\n%s\n请输出定居决策 JSON。",
                    task.getProductName(), task.getTargetMarket(), universeSummaries);

            Map<String, Object> parsed = JsonUtil.parseObject(
                    bailianManager.generateText(StageEnum.SETTLING, systemPrompt, userPrompt));
            if (parsed != null && parsed.get("selectedUniverseIndex") instanceof Number) {
                int idx = ((Number) parsed.get("selectedUniverseIndex")).intValue();
                UniverseDO llmChoice = universes.stream()
                        .filter(u -> u.getUniverseIndex() == idx).findFirst().orElse(null);
                if (llmChoice != null) {
                    selected = llmChoice;
                    rationale = String.valueOf(parsed.getOrDefault("rationale", rationale));
                    if (parsed.get("antiFragilePortfolio") instanceof List<?> list) {
                        list.forEach(item -> antiFragilePortfolio.add(String.valueOf(item)));
                    }
                    if (parsed.get("expectedProfit") instanceof Number n) expectedProfit = n.doubleValue();
                    if (parsed.get("confidence") instanceof Number c) confidence = c.doubleValue();
                }
            } else {
                log.warn("结算决策 JSON 解析失败，使用规则兜底 taskId={}", task.getId());
            }
        } catch (Exception e) {
            log.warn("结算决策 LLM 调用失败，使用规则兜底 taskId={}", task.getId(), e);
        }

        // 决策落库
        Map<String, Object> decisionData = new java.util.HashMap<>();
        decisionData.put("selectedUniverseId", selected.getId());
        decisionData.put("selectedUniverseIndex", selected.getUniverseIndex());
        decisionData.put("rationale", rationale);
        decisionData.put("antiFragilePortfolio", antiFragilePortfolio);
        decisionData.put("expectedProfit", expectedProfit);
        decisionData.put("confidence", confidence);
        SettlementDecisionDO decision = new SettlementDecisionDO();
        decision.setUniverseId(selected.getId());
        decision.setDecisionData(JsonUtil.toJson(decisionData));
        decision.setIsConfirmed(false);
        decision.setTraceId(task.getTraceId());
        settlementDecisionDAO.insert(decision);

        SettlementBO bo = new SettlementBO();
        bo.setUniverseId(selected.getId());
        bo.setSelectedStrategy(selected.getStrategyPackage());
        bo.setExpectedProfit(expectedProfit);
        bo.setConfidence(confidence);
        log.info("结算完成 taskId={} selectedUniverseId={} index={} rating={}",
                task.getId(), selected.getId(), selected.getUniverseIndex(), selected.getRating());
        return bo;
    }

    // ==================== 内部工具 ====================

    /** 合并规则证据(kb)与 LLM 推演证据(r1_inferred)，落实可解释推演的 source 标注 */
    private List<EvolutionResultBO.RuleEvidence> buildEvidences(
            EvolutionResultBO ruleResult, double llmScore, String reasoning) {
        List<EvolutionResultBO.RuleEvidence> evidences =
                ruleResult.getEvidences() == null ? new ArrayList<>() : new ArrayList<>(ruleResult.getEvidences());
        if (!Double.isNaN(llmScore) && reasoning != null && !reasoning.isBlank()) {
            EvolutionResultBO.RuleEvidence r1 = new EvolutionResultBO.RuleEvidence();
            r1.setRuleId("R1_INFERRED");
            r1.setInput("LLM 格局推演");
            r1.setOutput(reasoning);
            r1.setWeight(0.5);
            r1.setSource("r1_inferred");
            evidences.add(r1);
        }
        return evidences;
    }

    private UniverseBO toUniverseBO(UniverseDO universe) {
        UniverseBO bo = new UniverseBO();
        bo.setUniverseId(universe.getId());
        bo.setUniverseIndex(universe.getUniverseIndex());
        bo.setProductName(universe.getProductName());
        bo.setTargetMarket(universe.getTargetMarket());
        bo.setRating(parseRating(universe.getRating()));
        bo.setStrategyPackage(universe.getStrategyPackage());
        return bo;
    }

    private UniverseRatingEnum parseRating(String rating) {
        if (rating == null || rating.isBlank()) return null;
        try {
            return UniverseRatingEnum.valueOf(rating);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int ratingRank(String rating) {
        return switch (rating == null ? "" : rating) {
            case "A" -> 5;
            case "B" -> 4;
            case "C" -> 3;
            case "D" -> 2;
            case "F" -> 1;
            default -> 0;
        };
    }
}
