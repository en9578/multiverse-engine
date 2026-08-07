package com.minbao.multiverse.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTaskDTO {
    private String requestId;

    @NotBlank(message = "productName 不能为空")
    private String productName;

    @NotBlank(message = "targetMarket 不能为空")
    private String targetMarket;

    private String strategyDesc;
}