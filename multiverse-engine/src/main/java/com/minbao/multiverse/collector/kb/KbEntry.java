package com.minbao.multiverse.collector.kb;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * KB 知识库条目（对应 kb/*.yml 中 entries 列表的一条）。
 * 含 TTL 元数据（last_verified / freshness_ttl_days / verification_source），
 * 由 {@link KnowledgeBaseRegistry} 在启动时从 YAML 加载。
 */
@Data
public class KbEntry {
    private String id;
    private String name;
    /** PAIN_POINT | POLICY | COMPETITOR_STRATEGY */
    private String category;
    /** 市场码（ISO 3166 alpha-2）；ALL 或空 = 通用 */
    private String market;
    private List<String> keywords;
    /** high | medium | low | N/A */
    private String frequency;
    /** high | medium | low | N/A */
    private String severity;
    private String description;
    /** 竞品策略类：反应模式描述 */
    private String reactionPattern;
    private LocalDate lastVerified;
    private Integer freshnessTtlDays;
    private String verificationSource;
}
