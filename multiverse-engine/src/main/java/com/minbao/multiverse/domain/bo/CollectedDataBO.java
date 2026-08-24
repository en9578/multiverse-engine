package com.minbao.multiverse.domain.bo;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class CollectedDataBO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String productName;
    private String targetMarket;
    private Map<String, Object> competitorData;
    private Map<String, Object> complianceData;
    private Map<String, Object> reviewData;

    // ===== P3 数据源接入（design §8.1）：真实数据 + TTL 时效元数据 =====
    /** 汇率采集结果：{source, baseCurrency, quoteCurrency, rate, rates, rateDate, lastVerified, freshnessStatus, weight} */
    private Map<String, Object> exchangeRateData;
    /** KB 加载结果：{pain_points:[...], policies:[...], competitor_strategies:[...]}，每条内嵌 source/freshnessStatus/weight */
    private Map<String, Object> knowledgeBaseData = new HashMap<>();
    /** 数据源摘要列表（含 Tavily 降级原因），供前端展示每类数据来源与新鲜度 */
    private List<Map<String, Object>> dataSourceSummaries;
}
