package com.minbao.multiverse.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitDecisionDTO {
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    @NotNull(message = "universeId 不能为空")
    private Long universeId;
}