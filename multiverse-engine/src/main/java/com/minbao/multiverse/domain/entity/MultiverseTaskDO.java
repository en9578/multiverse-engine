package com.minbao.multiverse.domain.entity;

import com.minbao.multiverse.enums.TaskStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MultiverseTaskDO extends BaseDO {
    private String requestId;
    private TaskStatusEnum status;
    private String lastCompletedStage;
    private String productName;
    private String targetMarket;
    private String strategyDesc;
    private String resultJson;
    private Integer overallProgress;
}