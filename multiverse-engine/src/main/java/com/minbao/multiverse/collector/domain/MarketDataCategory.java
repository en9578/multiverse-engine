package com.minbao.multiverse.collector.domain;

/**
 * 市场数据类别（对应 market_data.category 枚举取值）。
 * EXCHANGE_RATE 汇率（frankfurter 实时 / 降级 MISSING）；
 * PAIN_POINT / POLICY / COMPETITOR_STRATEGY 三类 KB 静态知识库；
 * TAVILY 政策/竞品实时搜索（本轮无 key 降级，预留真实调用）。
 */
public enum MarketDataCategory {
    EXCHANGE_RATE,
    PAIN_POINT,
    POLICY,
    COMPETITOR_STRATEGY,
    TAVILY
}
