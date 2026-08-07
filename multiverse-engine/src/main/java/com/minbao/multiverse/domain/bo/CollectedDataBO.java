package com.minbao.multiverse.domain.bo;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class CollectedDataBO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String productName;
    private String targetMarket;
    private Map<String, Object> competitorData;
    private Map<String, Object> complianceData;
    private Map<String, Object> reviewData;
}
