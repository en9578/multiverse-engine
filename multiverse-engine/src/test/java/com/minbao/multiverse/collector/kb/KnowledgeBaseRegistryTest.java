package com.minbao.multiverse.collector.kb;

import com.minbao.multiverse.collector.domain.MarketDataCategory;
import com.minbao.multiverse.config.DataSourceProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P2 区域匹配：EU 政策条目命中 DE 任务；精确市场语义保留；representative 时效基准不变。
 */
class KnowledgeBaseRegistryTest {

    private final KnowledgeBaseRegistry registry;

    KnowledgeBaseRegistryTest() {
        registry = new KnowledgeBaseRegistry(new DataSourceProperties());
        registry.load(); // kb/*.yml 在 main resources，测试 classpath 可达
    }

    private Set<String> ids(MarketDataCategory category, String market) {
        return registry.findByCategoryAndMarket(category, market).stream()
                .map(KbEntry::getId).collect(Collectors.toSet());
    }

    @Test
    void dePolicyMatchesIncludingEuGpsr() {
        Set<String> ids = ids(MarketDataCategory.POLICY, "DE");
        assertTrue(ids.containsAll(List.of("PLCY-001", "PLCY-002", "PLCY-003")),
                () -> "DE 任务应命中 ElektroG/VerpackG + EU GPSR，实际: " + ids);
        assertFalse(ids.contains("PLCY-004"), "US CPSC 不应命中 DE");
    }

    @Test
    void exactMarketStillRespected() {
        Set<String> us = ids(MarketDataCategory.POLICY, "US");
        assertTrue(us.contains("PLCY-004"), "US 任务应命中 CPSC");
        assertFalse(us.contains("PLCY-003"), "EU GPSR 不应命中美国");
    }

    @Test
    void representativeForDeStaysPlcy002() {
        KbEntry rep = registry.representative(MarketDataCategory.POLICY, "DE");
        assertNotNull(rep);
        assertEquals("PLCY-002", rep.getId(),
                "DE 政策代表条目应仍为 last_verified 最新的 PLCY-002（时效基准不漂移）");
    }
}
