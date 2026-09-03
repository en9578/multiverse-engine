package com.minbao.multiverse.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class UniverseVO {
    private Long id;
    private Long taskId;
    private Integer universeIndex;
    /** 维度：TIME(时间宇宙) / STRATEGY(策略宇宙) */
    private String dimension;
    private String rating;
    private BigDecimal survivalRate;
    private String confidence;
    private String subState;
    private String strategyPackage;
    private Map<String, Object> evolutionData;
    private Map<String, Object> dataFreshness;
    private List<String> geneDefects;
    /** 5 风暴压力测试（仅 STRATEGY 宇宙有值，穿越体验舱雷达图数据源） */
    private List<StressTestVO> stressTests;
    /** 宇宙市场气象（仅 STRATEGY 宇宙有值；TIME 为 null） */
    private UniverseWeatherVO weather;
    /** 竞品关联反应（无 LLM 时为 []） */
    private List<CompetitorReactionVO> competitorReactions;
    /** 穿梭体验对话回复（仅 explore 接口返回） */
    private String reply;
    /** 穿梭体验会话 ID（多轮对话复用，仅 explore 接口返回） */
    private String sessionId;
}