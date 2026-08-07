package com.minbao.multiverse.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DecisionVO {
    private Long taskId;
    private Long universeId;
    private String decisionData;
    private BigDecimal confidence;
    private Boolean isConfirmed;
}