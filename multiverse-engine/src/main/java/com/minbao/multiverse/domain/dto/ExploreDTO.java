package com.minbao.multiverse.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExploreDTO {
    @NotBlank(message = "message 不能为空")
    private String message;

    private String sessionId;
}