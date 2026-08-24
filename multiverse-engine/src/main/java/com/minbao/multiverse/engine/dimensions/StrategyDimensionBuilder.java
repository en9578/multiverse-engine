package com.minbao.multiverse.engine.dimensions;

import com.minbao.multiverse.common.JsonUtil;
import com.minbao.multiverse.dao.GeneDefectDAO;
import com.minbao.multiverse.dao.UniverseDAO;
import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.domain.entity.GeneDefectDO;
import com.minbao.multiverse.domain.entity.MultiverseTaskDO;
import com.minbao.multiverse.domain.entity.UniverseDO;
import com.minbao.multiverse.enums.DimensionEnum;
import com.minbao.multiverse.enums.StageEnum;
import com.minbao.multiverse.manager.BailianManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 策略维度宇宙（设计 §3.3.2 / §3.4 步骤3）。
 * 定价(高端/性价比/低价引流) × 卖点(功能型/情感型/差异化型) × 定位(品类头部/细分垂直/价格破坏者)
 * 固定生成 5 个代表性策略宇宙（避免组合爆炸），并行 LLM（deepseek-v4-pro）生成策略包 + 基因缺陷。
 */
@Component
public class StrategyDimensionBuilder {
    private static final Logger log = LoggerFactory.getLogger(StrategyDimensionBuilder.class);

    /** 5 个代表策略组合（从 3×3×3 矩阵中选取，覆盖 3 定价 + 3 卖点 + 3 定位） */
    private record StrategyCombo(int index, String pricing, String sellingPoint, String positioning, String directive) {}

    private static final List<StrategyCombo> COMBOS = List.of(
            new StrategyCombo(1, "高端", "功能型", "品类头部",
                    "高端定价 + 功能型卖点 + 品类头部定位，主打品质与品牌溢价，围绕差异化功能与高客单价运营设计。"),
            new StrategyCombo(2, "高端", "差异化型", "细分垂直",
                    "高端定价 + 差异化卖点 + 细分垂直定位，切入高净值细分人群，围绕稀缺卖点与圈层运营设计。"),
            new StrategyCombo(3, "性价比", "功能型", "细分垂直",
                    "性价比定价 + 功能型卖点 + 细分垂直定位，主打功能扎实与价格适中，围绕供应链成本与垂直转化设计。"),
            new StrategyCombo(4, "性价比", "情感型", "品类头部",
                    "性价比定价 + 情感型卖点 + 品类头部定位，主打情感共鸣与大众走量，围绕内容营销与复购设计。"),
            new StrategyCombo(5, "低价引流", "差异化型", "价格破坏者",
                    "低价引流定价 + 差异化卖点 + 价格破坏者定位，主打极致低价与流量入口，围绕供应链成本与流量转化设计。"));

    @Resource private BailianManager bailianManager;
    @Resource private UniverseDAO universeDAO;
    @Resource private GeneDefectDAO geneDefectDAO;
    @Resource @Qualifier("bailianExecutor") private ThreadPoolTaskExecutor bailianExecutor;

    public List<UniverseBO> buildUniverses(MultiverseTaskDO task, CollectedDataBO data) {
        log.info("构建策略维度宇宙 taskId={}", task.getId());
        String systemPrompt = systemPrompt();

        List<CompletableFuture<UniverseBO>> futures = COMBOS.stream()
                .map(combo -> CompletableFuture.supplyAsync(
                        () -> buildOneUniverse(task, data, combo, systemPrompt), bailianExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<UniverseBO> universes = futures.stream().map(CompletableFuture::join).toList();
        log.info("策略维度宇宙完成 taskId={} count={}", task.getId(), universes.size());
        return universes;
    }

    private UniverseBO buildOneUniverse(MultiverseTaskDO task, CollectedDataBO data,
                                        StrategyCombo combo, String systemPrompt) {
        String userPrompt = String.format("""
                产品：%s（目标市场：%s）
                市场事实：%s
                策略组合指令：定价=%s，卖点=%s，定位=%s。%s
                请为该宇宙输出策略包 JSON。""",
                task.getProductName(), task.getTargetMarket(),
                JsonUtil.toJson(Map.of("competitors", data.getCompetitorData().get("competitors"),
                        "compliance", data.getComplianceData().get("compliance"),
                        "reviews", data.getReviewData())),
                combo.pricing(), combo.sellingPoint(), combo.positioning(), combo.directive());

        Map<String, Object> parsed = null;
        try {
            parsed = JsonUtil.parseObject(
                    bailianManager.generateText(StageEnum.GENERATING, systemPrompt, userPrompt));
        } catch (Exception e) {
            log.warn("策略宇宙 LLM 调用失败，降级为模板宇宙 taskId={} index={}", task.getId(), combo.index(), e);
        }
        if (parsed == null) {
            parsed = fallbackPackage(combo);
        }

        Map<String, Object> strategyPackage = parsed.get("strategyPackage") instanceof Map
                ? (Map<String, Object>) parsed.get("strategyPackage") : new HashMap<>();
        Map<String, Object> fullPackage = new HashMap<>(strategyPackage);
        fullPackage.put("dimension", DimensionEnum.STRATEGY.getCode());
        fullPackage.put("pricingStrategy", combo.pricing());
        fullPackage.put("sellingPointStrategy", combo.sellingPoint());
        fullPackage.put("positioningStrategy", combo.positioning());
        fullPackage.put("universeName", parsed.getOrDefault("universeName", combo.pricing() + "宇宙"));
        fullPackage.put("description", parsed.getOrDefault("description", ""));

        UniverseDO universe = new UniverseDO();
        universe.setTaskId(task.getId());
        universe.setUniverseIndex(combo.index());
        universe.setDimension(DimensionEnum.STRATEGY.getCode());
        universe.setProductName(task.getProductName());
        universe.setTargetMarket(task.getTargetMarket());
        universe.setRating("");
        universe.setSubState("GENERATED");
        universe.setStrategyPackage(JsonUtil.toJson(fullPackage));
        universe.setTraceId(task.getTraceId());
        universeDAO.insert(universe);

        insertGeneDefects(universe.getId(), parsed.get("geneDefects"), task.getTraceId());

        UniverseBO bo = new UniverseBO();
        bo.setUniverseId(universe.getId());
        bo.setUniverseIndex(combo.index());
        bo.setProductName(task.getProductName());
        bo.setTargetMarket(task.getTargetMarket());
        bo.setStrategyPackage(universe.getStrategyPackage());
        return bo;
    }

    private Map<String, Object> fallbackPackage(StrategyCombo combo) {
        Map<String, Object> pkg = new HashMap<>();
        pkg.put("universeName", combo.pricing() + "宇宙");
        pkg.put("description", combo.directive());
        pkg.put("positioning", combo.positioning());
        pkg.put("riskHedge", "无");
        pkg.put("keySellingPoints", List.of(combo.sellingPoint()));
        return pkg;
    }

    private void insertGeneDefects(Long universeId, Object geneDefects, String traceId) {
        if (!(geneDefects instanceof List<?> list)) return;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> defect)) continue;
            GeneDefectDO defectDO = new GeneDefectDO();
            defectDO.setUniverseId(universeId);
            defectDO.setDefectName(getStr(defect, "name", "未知缺陷"));
            defectDO.setFrequency(getStr(defect, "frequency", "medium"));
            defectDO.setSeverity(getStr(defect, "severity", "minor"));
            defectDO.setSolution(getStr(defect, "solution", ""));
            defectDO.setSourceTag("r1_inferred");
            defectDO.setTraceId(traceId);
            geneDefectDAO.insert(defectDO);
        }
    }

    private String getStr(Map<?, ?> map, String key, String defaultValue) {
        Object v = map.get(key);
        return v == null ? defaultValue : String.valueOf(v);
    }

    private String systemPrompt() {
        return """
                你是多元宇宙策略设计师。基于给定市场事实，为「定价×卖点×定位」策略组合设计完整策略包。
                只输出严格合法的 JSON，不要输出任何解释文字或 markdown 标记。JSON 结构：
                {"universeName":"宇宙名（10字内）","description":"设定一句话（30字内）","positioning":"定位一句话","price":29.9,"keySellingPoints":["卖点1","卖点2"],"riskHedge":"风险对冲手段","geneDefects":[{"name":"评论基因缺陷名","frequency":"high|medium|low","severity":"critical|major|minor","solution":"解决方案"}]}
                要求：price 必须是数字；geneDefects 给 2-4 条，来源为评论缺陷推断。""";
    }
}
