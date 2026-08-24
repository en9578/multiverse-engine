package com.minbao.multiverse.collector.domain;

import java.time.LocalDate;

/**
 * TTL 时效判定结果：状态 + 权重 + 是否过期 + source 标签 + 验证日期。
 * source 约定：FRESH→kb（全权重 1.0）、STALE→kb_stale（降权 0.5x）、MISSING→r1_inferred（纯 R1）。
 */
public record FreshnessInfo(
        FreshnessStatus status,
        double weight,
        boolean expired,
        String source,
        LocalDate lastVerified,
        int ttlDays) {
}
