package com.minbao.multiverse.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

/**
 * LLM 输出 JSON 容错解析工具。
 * 剥离 markdown code fence 与前后杂文本，提取首个完整 JSON 对象。
 * ObjectMapper 注册 JavaTimeModule 并输出 ISO 日期字符串（P3 market_data.raw_data 落库/展示）。
 */
public final class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtil() {}

    /**
     * 从 LLM 原始输出中提取首个完整 JSON 对象文本。
     * 支持：裸 JSON、```json ... ``` 包裹、前后混杂说明文字。
     */
    public static String extractJson(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        // 剥离 code fence
        if (s.contains("```")) {
            int start = s.indexOf("```");
            int firstLineEnd = s.indexOf('\n', start);
            if (firstLineEnd > 0) {
                int end = s.indexOf("```", firstLineEnd);
                if (end > firstLineEnd) {
                    s = s.substring(firstLineEnd + 1, end).trim();
                }
            }
        }
        // 定位首个 '{'，向后配平大括号
        int objStart = s.indexOf('{');
        if (objStart < 0) return null;
        int depth = 0;
        boolean inString = false;
        for (int i = objStart; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (c == '\\') { i++; continue; }
                if (c == '"') inString = false;
                continue;
            }
            if (c == '"') inString = true;
            else if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return s.substring(objStart, i + 1);
            }
        }
        return null;
    }

    /** 解析为 Map，失败返回 null */
    public static Map<String, Object> parseObject(String raw) {
        String json = extractJson(raw);
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    /** 序列化，失败返回空串 */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "";
        }
    }
}
