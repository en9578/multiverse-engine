package com.minbao.multiverse.engine.dimensions;

import com.minbao.multiverse.common.JsonUtil;
import com.minbao.multiverse.dao.UniverseDAO;
import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.domain.entity.MultiverseTaskDO;
import com.minbao.multiverse.domain.entity.UniverseDO;
import com.minbao.multiverse.enums.DimensionEnum;
import com.minbao.multiverse.enums.StageEnum;
import com.minbao.multiverse.manager.BailianManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 时间维度宇宙（设计 §3.3.1 / §3.4 步骤2 / §4.1）。
 * 穿越到 past_6m / now / future_3m 三个时间点，判断品类生命周期阶段并识别三类时间差机会
 * （地理时间差 / 趋势时间差 / 品类传导时间差），输出「时间差机会卡」。
 * LLM（deepseek-v4-pro）批量推理，解析失败降级为模板时间宇宙。
 */
@Component
public class TimeDimensionBuilder {
    private static final Logger log = LoggerFactory.getLogger(TimeDimensionBuilder.class);

    private static final List<String> TIME_POINTS = List.of("past_6m", "now", "future_3m");
    private static final Map<String, String> TIME_POINT_LABEL = Map.of(
            "past_6m", "穿越到6个月前", "now", "穿越到现在", "future_3m", "穿越到3个月后");

    @Resource private BailianManager bailianManager;
    @Resource private UniverseDAO universeDAO;

    public List<UniverseBO> buildUniverses(MultiverseTaskDO task, CollectedDataBO data) {
        log.info("构建时间维度宇宙 taskId={}", task.getId());
        List<Map<String, Object>> timePoints = new ArrayList<>();
        try {
            String raw = bailianManager.generateText(StageEnum.GENERATING, systemPrompt(), userPrompt(task, data));
            Map<String, Object> parsed = JsonUtil.parseObject(raw);
            if (parsed != null && parsed.get("timeUniverses") instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        timePoints.add(stringKeyMap(m));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("时间维度 LLM 调用失败，降级为模板时间宇宙 taskId={}", task.getId(), e);
        }
        if (timePoints.isEmpty()) {
            timePoints.addAll(fallbackTimePoints());
        }

        List<UniverseBO> result = new ArrayList<>();
        int index = 1;
        for (String tp : TIME_POINTS) {
            Map<String, Object> item = findTimePoint(timePoints, tp);
            result.add(persistTimeUniverse(task, data, item, index++, tp));
        }
        log.info("时间维度宇宙完成 taskId={} count={}", task.getId(), result.size());
        return result;
    }

    private UniverseBO persistTimeUniverse(MultiverseTaskDO task, CollectedDataBO data,
                                           Map<String, Object> item, int index, String timePoint) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("dimension", DimensionEnum.TIME.getCode());
        card.put("timePoint", timePoint);
        card.put("lifecycleStage", str(item.get("lifecycleStage"), "起势"));
        card.put("opportunityType", str(item.get("opportunityType"), "趋势时间差"));
        card.put("opportunityWindow", str(item.get("opportunityWindow"), ""));
        card.put("urgency", str(item.get("urgency"), "medium"));

        UniverseDO universe = new UniverseDO();
        universe.setTaskId(task.getId());
        universe.setUniverseIndex(index);
        universe.setDimension(DimensionEnum.TIME.getCode());
        universe.setProductName(task.getProductName());
        universe.setTargetMarket(task.getTargetMarket());
        universe.setRating("");
        universe.setSubState("GENERATED");
        universe.setStrategyPackage(JsonUtil.toJson(card));
        universe.setTraceId(task.getTraceId());
        universeDAO.insert(universe);

        UniverseBO bo = new UniverseBO();
        bo.setUniverseId(universe.getId());
        bo.setUniverseIndex(index);
        bo.setProductName(task.getProductName());
        bo.setTargetMarket(task.getTargetMarket());
        bo.setStrategyPackage(universe.getStrategyPackage());
        return bo;
    }

    private Map<String, Object> findTimePoint(List<Map<String, Object>> list, String tp) {
        return list.stream().filter(m -> tp.equals(str(m.get("timePoint"), "")))
                .findFirst().orElseGet(Map::of);
    }

    private List<Map<String, Object>> fallbackTimePoints() {
        return List.of(
                Map.of("timePoint", "past_6m", "lifecycleStage", "饱和",
                        "opportunityType", "地理时间差", "opportunityWindow", "已红海，判断当时可预判信号", "urgency", "low"),
                Map.of("timePoint", "now", "lifecycleStage", "起势",
                        "opportunityType", "趋势时间差", "opportunityWindow", "当前起势未饱和，切入窗口", "urgency", "high"),
                Map.of("timePoint", "future_3m", "lifecycleStage", "爆发",
                        "opportunityType", "品类传导时间差", "opportunityWindow", "下游配件随上游品类跟涨", "urgency", "medium"));
    }

    private String systemPrompt() {
        return """
                你是跨境电商「时间旅行选品官」（场景五方向①）。基于产品与目标市场事实，穿越到 3 个时间点判断该品类生命周期阶段，并识别时间差机会。
                只输出严格合法的 JSON，不要输出任何解释文字或 markdown 标记。JSON 结构：
                {"timeUniverses":[
                  {"timePoint":"past_6m","lifecycleStage":"起势|爆发|饱和","opportunityType":"地理时间差|趋势时间差|品类传导时间差","opportunityWindow":"机会窗口一句话","urgency":"high|medium|low"},
                  {"timePoint":"now","lifecycleStage":"...","opportunityType":"...","opportunityWindow":"...","urgency":"..."},
                  {"timePoint":"future_3m","lifecycleStage":"...","opportunityType":"...","opportunityWindow":"...","urgency":"..."}
                ]}
                要求：恰好 3 个条目，timePoint 依次为 past_6m / now / future_3m；opportunityType 从三类时间差中选择；urgency 为机会紧迫度。""";
    }

    private String userPrompt(MultiverseTaskDO task, CollectedDataBO data) {
        return String.format("""
                产品：%s（目标市场：%s）
                市场事实：%s
                请穿越到 6 个月前 / 现在 / 3 个月后三个时间点，输出时间差机会 JSON。""",
                task.getProductName(), task.getTargetMarket(),
                JsonUtil.toJson(Map.of("competitors", data.getCompetitorData().get("competitors"),
                        "compliance", data.getComplianceData().get("compliance"),
                        "reviews", data.getReviewData())));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> stringKeyMap(Map<?, ?> m) {
        Map<String, Object> out = new LinkedHashMap<>();
        m.forEach((k, v) -> out.put(String.valueOf(k), v));
        return out;
    }

    private String str(Object v, String def) { return v == null ? def : v.toString(); }
}
