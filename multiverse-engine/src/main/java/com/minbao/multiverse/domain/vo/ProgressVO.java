package com.minbao.multiverse.domain.vo;

import com.minbao.multiverse.enums.TaskStatusEnum;
import lombok.Data;

import java.util.List;

@Data
public class ProgressVO {
    private Long taskId;
    private TaskStatusEnum status;
    private Integer overallProgress;
    private String currentStage;
    private List<UniverseProgress> universeProgress;

    @Data
    public static class UniverseProgress {
        private Integer index;
        private String subState;
        private String rating;
        private Boolean degraded;
    }
}