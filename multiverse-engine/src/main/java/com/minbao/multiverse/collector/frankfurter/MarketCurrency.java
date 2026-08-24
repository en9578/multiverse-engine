package com.minbao.multiverse.collector.frankfurter;

import java.util.Map;

/**
 * 目标市场（ISO 3166 alpha-2）→ ISO 4217 结算货币映射，用于汇率采集的基准货币推导。
 * 未知市场回退 USD（与 SETTLING 阶段 expectedProfit 的美元单位一致）。
 */
public final class MarketCurrency {
    private static final Map<String, String> MAPPING = Map.ofEntries(
            Map.entry("US", "USD"),
            Map.entry("DE", "EUR"), Map.entry("FR", "EUR"), Map.entry("IT", "EUR"),
            Map.entry("ES", "EUR"), Map.entry("NL", "EUR"), Map.entry("BE", "EUR"),
            Map.entry("GB", "GBP"),
            Map.entry("JP", "JPY"),
            Map.entry("CA", "CAD"),
            Map.entry("AU", "AUD"),
            Map.entry("CN", "CNY"));

    private MarketCurrency() {
    }

    public static String resolve(String targetMarket) {
        if (targetMarket == null || targetMarket.isBlank()) {
            return "USD";
        }
        return MAPPING.getOrDefault(targetMarket.trim().toUpperCase(), "USD");
    }
}
