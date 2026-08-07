package com.minbao.multiverse.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntanglementEdgeDO extends BaseDO {
    private Long sourceUniverseId;
    private Long targetUniverseId;
    private Double weight;
}
