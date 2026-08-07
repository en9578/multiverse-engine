package com.minbao.multiverse.domain.bo;

import lombok.Data;

import java.io.Serializable;

@Data
public class SettlementBO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long universeId;
    private String selectedStrategy;
    private Double expectedProfit;
    private Double confidence;
}
