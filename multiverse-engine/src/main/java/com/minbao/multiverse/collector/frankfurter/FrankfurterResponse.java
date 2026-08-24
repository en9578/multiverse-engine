package com.minbao.multiverse.collector.frankfurter;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.Map;

/**
 * frankfurter.dev 汇率响应（示例：{"amount":1.0,"base":"EUR","date":"2026-08-24","rates":{"USD":1.1664}}）。
 * date 用 String 接收，避免依赖 Jackson JavaTimeModule。
 */
public record FrankfurterResponse(
        @JsonProperty("amount") double amount,
        @JsonProperty("base") String base,
        @JsonProperty("date") String date,
        @JsonProperty("rates") Map<String, Double> rates) {

    public LocalDate getRateDate() {
        return LocalDate.parse(date);
    }
}
