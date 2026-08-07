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
    private String rating;
    private BigDecimal survivalRate;
    private String confidence;
    private String subState;
    private String strategyPackage;
    private Map<String, Object> evolutionData;
    private Map<String, Object> dataFreshness;
    private List<String> geneDefects;
}