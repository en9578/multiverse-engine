package com.minbao.multiverse.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneDefectDO extends BaseDO {
    private Long universeId;
    private String defectName;
    private String frequency;
    private String severity;
    private String solution;
    private String sourceTag;
}