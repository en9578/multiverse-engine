package com.minbao.multiverse.collector;

import com.minbao.multiverse.collector.domain.FreshnessInfo;
import com.minbao.multiverse.collector.domain.FreshnessStatus;
import com.minbao.multiverse.collector.domain.MarketDataCategory;
import com.minbao.multiverse.collector.frankfurter.FrankfurterClient;
import com.minbao.multiverse.collector.frankfurter.FrankfurterResponse;
import com.minbao.multiverse.collector.frankfurter.MarketCurrency;
import com.minbao.multiverse.collector.kb.KbEntry;
import com.minbao.multiverse.collector.kb.KnowledgeBaseRegistry;
import com.minbao.multiverse.collector.tavily.TavilyClient;
import com.minbao.multiverse.common.JsonUtil;
import com.minbao.multiverse.dao.MarketDataDAO;
import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.entity.MarketDataDO;
import com.minbao.multiverse.domain.entity.MultiverseTaskDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * P3 数据源采集核心（design §8.1）。
 * 先于 LLM 采集真实数据源（frankfurter 汇率 + KB 三类 + Tavily 降级）并落库 market_data，
 * 保证 LLM 配额不足/后续阶段失败时采集数据已持久化、可独立展示。
 * 产出增强版 {@link CollectedDataBO}（含 TTL 时效元数据），完全不依赖 LLM。
 */
@Service
public class DataCollector {
    private static final Logger log = LoggerFactory.getLogger(DataCollector.class);

    private static final List<String> QUOTE_SYMBOLS = List.of("USD", "CNY");
    /** 实时汇率 TTL 日级 */
    private static final int FX_TTL_DAYS = 1;

    @Resource private FrankfurterClient frankfurterClient;
    @Resource private KnowledgeBaseRegistry kbRegistry;
    @Resource private TavilyClient tavilyClient;
    @Resource private DataFreshnessService freshnessService;
    @Resource private MarketDataDAO marketDataDAO;

    public CollectedDataBO collect(MultiverseTaskDO task) {
        CollectedDataBO data = new CollectedDataBO();
        data.setProductName(task.getProductName());
        data.setTargetMarket(task.getTargetMarket());

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> summaries = new ArrayList<>();

        // 1) 汇率：frankfurter 实时，失败降级 Missing（纯 R1）
        Map<String, Object> fx = collectExchangeRate(task, today);
        data.setExchangeRateData(fx);
        summaries.add(buildSummary(MarketDataCategory.EXCHANGE_RATE, fx));
        persist(task, MarketDataCategory.EXCHANGE_RATE,
                String.valueOf(fx.get("source")), String.valueOf(fx.get("freshnessStatus")),
                FX_TTL_DAYS, (double) fx.get("weight"),
                (LocalDate) fx.get("lastVerified"), JsonUtil.toJson(fx));

        // 2) KB 三类（痛点/政策/竞品策略），每类以代表条目定 TTL 状态
        Map<String, Object> kb = new LinkedHashMap<>();
        for (MarketDataCategory cat : List.of(MarketDataCategory.PAIN_POINT,
                MarketDataCategory.POLICY, MarketDataCategory.COMPETITOR_STRATEGY)) {
            List<KbEntry> entries = kbRegistry.findByCategoryAndMarket(cat, task.getTargetMarket());
            KbEntry rep = kbRegistry.representative(cat, task.getTargetMarket());
            FreshnessInfo fi = rep == null
                    ? freshnessService.evaluate(null, defaultTtlDays(cat), today)
                    : freshnessService.evaluate(rep.getLastVerified(), rep.getFreshnessTtlDays(), today);

            List<Map<String, Object>> entryMaps = entries.stream()
                    .map(e -> toEntryMap(e, fi))
                    .toList();
            kb.put(kbKey(cat), entryMaps);

            persist(task, cat, fi.source(), fi.status().name(),
                    rep == null ? defaultTtlDays(cat) : rep.getFreshnessTtlDays(),
                    fi.weight(), rep == null ? null : rep.getLastVerified(),
                    JsonUtil.toJson(entries));

            Map<String, Object> summary = new HashMap<>();
            summary.put("count", entries.size());
            summary.put("source", fi.source());
            summary.put("freshnessStatus", fi.status().name());
            summary.put("lastVerified", rep == null ? null : rep.getLastVerified().toString());
            summary.put("weight", fi.weight());
            summaries.add(buildSummary(cat, summary));
            log.info("KB 采集 category={} count={} freshness={} weight={} taskId={}",
                    cat, entries.size(), fi.status(), fi.weight(), task.getId());
        }
        data.setKnowledgeBaseData(kb);

        // 3) Tavily 实时搜索（本轮降级 stub）→ 落库展示数据源状态（未配置 key 时 MISSING + KB 兜底）
        TavilyClient.TavilySearchResult tavily = tavilyClient.search(
                task.getTargetMarket() + " " + task.getProductName() + " 政策/竞品实时搜索");
        Map<String, Object> tavilyInfo = new LinkedHashMap<>();
        tavilyInfo.put("source", "tavily");
        if (tavily.enabled()) {
            tavilyInfo.put("freshnessStatus", FreshnessStatus.FRESH.name());
            tavilyInfo.put("weight", FreshnessStatus.FRESH.getWeight());
            tavilyInfo.put("lastVerified", today);
            tavilyInfo.put("count", tavily.results().size());
            persist(task, MarketDataCategory.TAVILY, "tavily", FreshnessStatus.FRESH.name(),
                    1, 1.0, today, JsonUtil.toJson(tavily.results()));
            log.info("Tavily 搜索完成 taskId={} count={}", task.getId(), tavily.results().size());
        } else {
            tavilyInfo.put("freshnessStatus", FreshnessStatus.MISSING.name());
            tavilyInfo.put("weight", FreshnessStatus.MISSING.getWeight());
            tavilyInfo.put("reason", tavily.reason());
            persist(task, MarketDataCategory.TAVILY, "tavily", FreshnessStatus.MISSING.name(),
                    1, 0.0, null, JsonUtil.toJson(Map.of("reason", tavily.reason())));
            log.info("Tavily 降级 taskId={} reason={}", task.getId(), tavily.reason());
        }
        summaries.add(buildSummary(MarketDataCategory.TAVILY, tavilyInfo));

        data.setDataSourceSummaries(summaries);
        log.info("数据采集完成 taskId={} fx={} summaries={}",
                task.getId(), fx.get("freshnessStatus"), summaries.size());
        return data;
    }

    // ==================== 汇率 ====================

    private Map<String, Object> collectExchangeRate(MultiverseTaskDO task, LocalDate today) {
        String base = MarketCurrency.resolve(task.getTargetMarket());
        Optional<FrankfurterResponse> fx = frankfurterClient.fetchLatest(base, QUOTE_SYMBOLS);
        if (fx.isPresent()) {
            FrankfurterResponse resp = fx.get();
            // from 与 to 相同时 frankfurter 不返回 base 货币本身，美元对美元恒为 1.0
            double usdRate = "USD".equalsIgnoreCase(base) ? 1.0 : resp.rates().getOrDefault("USD", 0.0);
            log.info("汇率采集 frankfurter {}/USD={} FRESH taskId={}", base, usdRate, task.getId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("source", "frankfurter");
            m.put("baseCurrency", base);
            m.put("quoteCurrency", "USD");
            m.put("rate", usdRate);
            m.put("rates", resp.rates());
            m.put("rateDate", resp.date());
            m.put("lastVerified", today);
            m.put("freshnessStatus", FreshnessStatus.FRESH.name());
            m.put("weight", FreshnessStatus.FRESH.getWeight());
            return m;
        }
        log.warn("汇率实时源不可用，降级 Missing 纯 R1 taskId={} base={}", task.getId(), base);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("source", "r1_inferred");
        m.put("baseCurrency", base);
        m.put("quoteCurrency", "USD");
        m.put("rate", 0.0);
        m.put("lastVerified", null);
        m.put("freshnessStatus", FreshnessStatus.MISSING.name());
        m.put("weight", FreshnessStatus.MISSING.getWeight());
        return m;
    }

    // ==================== KB 条目 → Map ====================

    private Map<String, Object> toEntryMap(KbEntry e, FreshnessInfo fi) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("name", e.getName());
        m.put("market", e.getMarket());
        m.put("frequency", e.getFrequency());
        m.put("severity", e.getSeverity());
        m.put("description", e.getDescription());
        if (e.getReactionPattern() != null) {
            m.put("reactionPattern", e.getReactionPattern());
        }
        m.put("source", fi.source());
        m.put("freshnessStatus", fi.status().name());
        m.put("weight", fi.weight());
        m.put("lastVerified", e.getLastVerified() == null ? null : e.getLastVerified().toString());
        return m;
    }

    private String kbKey(MarketDataCategory cat) {
        return switch (cat) {
            case PAIN_POINT -> "pain_points";
            case POLICY -> "policies";
            case COMPETITOR_STRATEGY -> "competitor_strategies";
            default -> cat.name().toLowerCase();
        };
    }

    private int defaultTtlDays(MarketDataCategory cat) {
        return switch (cat) {
            case PAIN_POINT -> 30;
            case POLICY, COMPETITOR_STRATEGY -> 90;
            default -> 30;
        };
    }

    // ==================== 落库 / 摘要 ====================

    /** market_data 一行 = (task_id, category)，UNIQUE KEY 幂等 upsert（retry 覆盖） */
    private void persist(MultiverseTaskDO task, MarketDataCategory category, String source,
                         String status, int ttlDays, double weight, LocalDate lastVerified, String rawJson) {
        MarketDataDO row = new MarketDataDO();
        row.setTaskId(task.getId());
        row.setCategory(category.name());
        row.setSource(source);
        row.setFreshnessStatus(status);
        row.setFreshnessTtlDays(ttlDays);
        row.setWeight(java.math.BigDecimal.valueOf(weight));
        row.setLastVerified(lastVerified);
        row.setRawData(rawJson);
        row.setTraceId(task.getTraceId());
        marketDataDAO.upsert(row);
    }

    /** 数据源摘要（含前端展示所需 source/freshness/weight/count） */
    private Map<String, Object> buildSummary(MarketDataCategory category, Map<String, Object> extra) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("category", category.name());
        m.putAll(extra);
        return m;
    }
}
