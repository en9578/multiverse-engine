package com.minbao.multiverse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * P3 数据源配置（application.yml 的 data-source 块）。
 * frankfurter 汇率 API 免费无 key；tavily 需 TAVILY_API_KEY，未配置时自动降级 KB 兜底。
 */
@Data
@ConfigurationProperties(prefix = "data-source")
public class DataSourceProperties {
    private Frankfurter frankfurter = new Frankfurter();
    private Tavily tavily = new Tavily();
    private Kb kb = new Kb();

    @Data
    public static class Frankfurter {
        /** 官方端点（.app 已 301 到 .dev/v1），RestClient 默认不跟随跨域重定向 */
        private String baseUrl = "https://api.frankfurter.dev/v1";
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 5000;
    }

    @Data
    public static class Tavily {
        /** 环境变量注入；空串表示未配置，走降级 */
        private String apiKey = "";
        private String baseUrl = "https://api.tavily.com";
    }

    @Data
    public static class Kb {
        private String painPointFile = "pain_point_kb.yml";
        private String policyFile = "policy_kb.yml";
        private String competitorStrategyFile = "competitor_strategy_kb.yml";
    }
}
