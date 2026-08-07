package com.minbao.multiverse.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfigBean {

    @Bean
    public CircuitBreaker bailianBreaker() {
        return CircuitBreaker.of("bailian", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(60)
                .slowCallDurationThreshold(Duration.ofSeconds(30))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(20)
                .build());
    }
}
