package com.minbao.multiverse.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * 市场数据采集结果（P3 数据源接入，设计 §8.1）。
 * 一行 = (task_id, category)，持久化采集原始数据 + TTL 三层时效元数据，
 * 支持前端展示 last_verified 与 Fresh/Stale 状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MarketDataDO extends BaseDO {
    private Long taskId;
    /** EXCHANGE_RATE | PAIN_POINT | POLICY | COMPETITOR_STRATEGY */
    private String category;
    /** frankfurter | kb | kb_stale | tavily | r1_inferred */
    private String source;
    /** 原始采集数据 JSON（汇率 rates / KB 条目列表） */
    private String rawData;
    private LocalDate lastVerified;
    private Integer freshnessTtlDays;
    /** FRESH | STALE | MISSING */
    private String freshnessStatus;
    private BigDecimal weight;
}
