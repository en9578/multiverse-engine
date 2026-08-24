package com.minbao.multiverse.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class UniverseDO extends BaseDO {
    private Long taskId;
    private Integer universeIndex;
    /** 维度：TIME(时间宇宙) / STRATEGY(策略宇宙) */
    private String dimension;
    private String productName;
    private String targetMarket;
    private String rating;
    private String subState;
    private String evolutionData;
    private BigDecimal survivalRate;
    private String strategyPackage;
}