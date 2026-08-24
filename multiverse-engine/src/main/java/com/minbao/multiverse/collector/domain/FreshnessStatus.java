package com.minbao.multiverse.collector.domain;

import lombok.Getter;

/**
 * 数据新鲜度三层（设计 §22.2）：
 * FRESH 全权重 1.0 / STALE 降权 0.5x / MISSING 纯 R1（权重 0）。
 */
@Getter
public enum FreshnessStatus {
    FRESH(1.0),
    STALE(0.5),
    MISSING(0.0);

    private final double weight;

    FreshnessStatus(double weight) {
        this.weight = weight;
    }
}
