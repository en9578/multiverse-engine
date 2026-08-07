package com.minbao.multiverse.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExplorationDTO {
    @NotNull
    private Long universeId;

    private List<String> dimensions;
}
