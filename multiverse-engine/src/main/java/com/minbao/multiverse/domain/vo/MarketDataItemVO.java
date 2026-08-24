package com.minbao.multiverse.domain.vo;

import lombok.Data;

/**
 * 采集数据单条展示项（含 TTL 时效元数据 + 可读文案）。
 */
@Data
public class MarketDataItemVO {
    /** EXCHANGE_RATE | PAIN_POINT | POLICY | COMPETITOR_STRATEGY */
    private String category;
    /** frankfurter | kb | kb_stale | tavily | r1_inferred */
    private String source;
    private String lastVerified;
    private Integer freshnessTtlDays;
    /** FRESH | STALE | MISSING */
    private String freshnessStatus;
    private Double weight;
    /** 可读文案，如 "政策 3 条，来源 KB，最后验证 2026-04-15，Stale 降权 0.5x" */
    private String display;
    /** 原始采集数据（反序列化后的 JSON） */
    private Object rawData;
}
