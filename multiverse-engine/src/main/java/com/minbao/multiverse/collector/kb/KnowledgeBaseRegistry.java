package com.minbao.multiverse.collector.kb;

import com.minbao.multiverse.collector.domain.MarketDataCategory;
import com.minbao.multiverse.config.DataSourceProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * KB 静态知识库注册中心（P3 数据源）。
 * 启动时用 SnakeYAML 加载 kb/pain_point_kb.yml、policy_kb.yml、competitor_strategy_kb.yml（Boot 自带 snakeyaml，零新依赖），
 * 支持按 category + market 过滤查询，并提供「代表条目」（last_verified 最新的一条）作为该类别 TTL 判定基准。
 */
@Component
public class KnowledgeBaseRegistry {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseRegistry.class);

    private final DataSourceProperties props;
    private final Map<MarketDataCategory, List<KbEntry>> entriesByCategory = new EnumMap<>(MarketDataCategory.class);

    public KnowledgeBaseRegistry(DataSourceProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void load() {
        entriesByCategory.put(MarketDataCategory.PAIN_POINT,
                loadFile(props.getKb().getPainPointFile()));
        entriesByCategory.put(MarketDataCategory.POLICY,
                loadFile(props.getKb().getPolicyFile()));
        entriesByCategory.put(MarketDataCategory.COMPETITOR_STRATEGY,
                loadFile(props.getKb().getCompetitorStrategyFile()));
        entriesByCategory.forEach((cat, entries) ->
                log.info("KB 加载完成 category={} count={}", cat, entries.size()));
    }

    private List<KbEntry> loadFile(String file) {
        try (InputStream is = new ClassPathResource("kb/" + file).getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(is);
            Object rawEntries = root == null ? null : root.get("entries");
            if (!(rawEntries instanceof List<?> list)) {
                log.warn("KB 文件无 entries 列表 file={}", file);
                return List.of();
            }
            List<KbEntry> result = new ArrayList<>();
            for (Object raw : list) {
                if (raw instanceof Map<?, ?> m) {
                    result.add(toEntry(m));
                }
            }
            return result;
        } catch (IOException | RuntimeException e) {
            log.warn("KB 文件加载失败 file={} err={}", file, e.getMessage());
            return List.of();
        }
    }

    /** SnakeYAML 产物 Map → KbEntry（手写映射，避免引入 jackson-dataformat-yaml） */
    @SuppressWarnings("unchecked")
    private KbEntry toEntry(Map<?, ?> m) {
        KbEntry e = new KbEntry();
        e.setId(str(m.get("id")));
        e.setName(str(m.get("name")));
        e.setCategory(str(m.get("category")));
        e.setMarket(str(m.get("market")));
        e.setKeywords(m.get("keywords") instanceof List<?> k
                ? k.stream().map(String::valueOf).toList() : List.of());
        e.setFrequency(str(m.get("frequency")));
        e.setSeverity(str(m.get("severity")));
        e.setDescription(str(m.get("description")));
        e.setReactionPattern(str(m.get("reaction_pattern")));
        e.setLastVerified(parseDate(m.get("last_verified")));
        e.setFreshnessTtlDays(m.get("freshness_ttl_days") instanceof Number n ? n.intValue() : 30);
        e.setVerificationSource(str(m.get("verification_source")));
        return e;
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private LocalDate parseDate(Object o) {
        if (o == null) return null;
        // SnakeYAML 会把 ISO 日期隐式解析为 java.util.Date（UTC 午夜），需兼容
        if (o instanceof Date d) {
            return d.toInstant().atZone(TimeZone.getDefault().toZoneId()).toLocalDate();
        }
        try {
            return LocalDate.parse(String.valueOf(o));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** 按类别 + 市场过滤（market 空/ALL 放行，大小写不敏感）；无匹配返回空列表 */
    public List<KbEntry> findByCategoryAndMarket(MarketDataCategory category, String market) {
        List<KbEntry> all = entriesByCategory.getOrDefault(category, List.of());
        if (market == null || market.isBlank()) {
            return all;
        }
        String m = market.trim().toUpperCase();
        return all.stream()
                .filter(e -> e.getMarket() == null || e.getMarket().isBlank()
                        || "ALL".equalsIgnoreCase(e.getMarket())
                        || e.getMarket().equalsIgnoreCase(m))
                .toList();
    }

    /** 代表条目：匹配条目中 last_verified 最新的一条，作为该类别 TTL 判定基准 */
    public KbEntry representative(MarketDataCategory category, String market) {
        return findByCategoryAndMarket(category, market).stream()
                .filter(e -> e.getLastVerified() != null)
                .max(java.util.Comparator.comparing(KbEntry::getLastVerified))
                .orElse(null);
    }
}
