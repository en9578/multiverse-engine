package com.minbao.multiverse.domain.vo;

import com.minbao.multiverse.enums.TaskStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskVO {
    private Long id;
    private String requestId;
    private TaskStatusEnum status;
    private Integer overallProgress;
    private String productName;
    private String targetMarket;
    private Integer universeCount;
    private Integer exploredCount;
    private Integer failedCount;
    private String confidence;
    private LocalDateTime gmtCreate;
    private List<TaskError> errors;

    @Data
    public static class TaskError {
        private Integer universeIndex;
        private String message;
        private Boolean degraded;
    }
}