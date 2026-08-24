package com.minbao.multiverse.collector.frankfurter;

import com.minbao.multiverse.config.DataSourceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

/**
 * frankfurter.dev 汇率客户端（免费、无 key，日级时效）。
 * 同步命令式采集用 Boot 3.4 内置 RestClient（零新增依赖）。
 * 任何失败返回 Optional.empty()（设计内降级），不抛异常中断编排。
 */
@Component
public class FrankfurterClient {
    private static final Logger log = LoggerFactory.getLogger(FrankfurterClient.class);

    private final RestClient restClient;

    public FrankfurterClient(DataSourceProperties props) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(props.getFrankfurter().getConnectTimeoutMs());
        requestFactory.setReadTimeout(props.getFrankfurter().getReadTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(props.getFrankfurter().getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /** GET /latest?from={from}&to={symbols}，失败/空响应返回 Optional.empty() */
    public Optional<FrankfurterResponse> fetchLatest(String from, List<String> symbols) {
        try {
            FrankfurterResponse resp = restClient.get()
                    .uri(ub -> ub.path("/latest")
                            .queryParam("from", from)
                            .queryParam("to", String.join(",", symbols))
                            .build())
                    .retrieve()
                    .body(FrankfurterResponse.class);
            if (resp == null || resp.rates() == null || resp.rates().isEmpty()) {
                log.warn("frankfurter 返回空响应 from={} to={}", from, symbols);
                return Optional.empty();
            }
            return Optional.of(resp);
        } catch (Exception e) {
            log.warn("frankfurter 调用失败 from={} to={} err={}", from, symbols, e.getMessage());
            return Optional.empty();
        }
    }
}
