package com.minbao.multiverse.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 数据采集效果展示聚合（GET /api/v1/tasks/{id}/collected-data）。
 * items 逐条展示每类数据（汇率/KB 三类/Tavily）的来源 + 新鲜度 + 原始数据。
 */
@Data
public class CollectedDataVO {
    private Long taskId;
    private String productName;
    private String targetMarket;
    private List<MarketDataItemVO> items;
}
