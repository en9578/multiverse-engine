package com.minbao.multiverse.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SettlementDecisionDO extends BaseDO {
    private Long universeId;
    private String decisionData;
    private Boolean isConfirmed;
}