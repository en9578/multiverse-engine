package com.minbao.multiverse.collector;

import com.minbao.multiverse.collector.domain.FreshnessInfo;
import com.minbao.multiverse.collector.domain.FreshnessStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * TTL 三层时效判定（设计 §22.2）：
 * last_verified + ttl_days > 今天 → Fresh 全权重；
 * 否则 → Stale 降权 0.5x；last_verified 缺失 → Missing 纯 R1。
 */
@Service
public class DataFreshnessService {

    public FreshnessInfo evaluate(LocalDate lastVerified, int ttlDays, LocalDate today) {
        if (lastVerified == null) {
            return new FreshnessInfo(FreshnessStatus.MISSING, 0.0, true, "r1_inferred", null, 0);
        }
        LocalDate expireAt = lastVerified.plusDays(ttlDays);
        if (expireAt.isAfter(today)) {
            return new FreshnessInfo(FreshnessStatus.FRESH, 1.0, false, "kb", lastVerified, ttlDays);
        }
        return new FreshnessInfo(FreshnessStatus.STALE, 0.5, true, "kb_stale", lastVerified, ttlDays);
    }
}
