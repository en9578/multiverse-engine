package com.minbao.multiverse.collector.tavily;

import com.minbao.multiverse.config.DataSourceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tavily 搜索客户端（政策/竞品实时搜索，免费 1000 次/月）。
 * 本轮为降级 stub：未配置 TAVILY_API_KEY 时跳过实时搜索返回 DISABLED（KB 预设兜底），
 * key 到位后在 search() 预留位置实现 POST https://api.tavily.com/search。
 */
@Component
public class TavilyClient {
    private static final Logger log = LoggerFactory.getLogger(TavilyClient.class);

    private final String apiKey;
    private final String baseUrl;

    public TavilyClient(DataSourceProperties props) {
        this.apiKey = props.getTavily().getApiKey();
        this.baseUrl = props.getTavily().getBaseUrl();
    }

    /** 搜索降级结果：enabled=false 表示未启用真实搜索，reason 说明原因 */
    public record TavilySearchResult(boolean enabled, List<Map<String, Object>> results, String reason) {
        static final TavilySearchResult DISABLED =
                new TavilySearchResult(false, List.of(), "TAVILY_API_KEY 未配置，本轮跳过实时搜索，KB 兜底");
    }

    public TavilySearchResult search(String query) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Tavily 未配置 api-key，跳过实时搜索 query={}", query);
            return TavilySearchResult.DISABLED;
        }
        // 预留：配置 TAVILY_API_KEY 后在此实现 POST {baseUrl}/search
        //   body: {"query": query, "search_depth": "basic", "max_results": 5}
        //   header: Authorization: Bearer {apiKey}
        log.warn("Tavily 真实调用待实现（本轮降级）query={} baseUrl={}", query, baseUrl);
        return TavilySearchResult.DISABLED;
    }
}
