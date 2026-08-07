package com.minbao.multiverse.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component("engineHealth")
public class EngineHealthIndicator extends AbstractHealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(EngineHealthIndicator.class);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                builder.up().withDetail("engine", "reachable");
            } else {
                builder.down().withDetail("engine", "status=" + response.statusCode());
            }
        } catch (Exception e) {
            builder.down().withDetail("engine", "unreachable: " + e.getMessage());
        }
    }
}