package com.minbao.multiverse.engine.dimensions;

import com.minbao.multiverse.dao.UniverseWeatherDAO;
import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.entity.UniverseWeatherDO;
import com.minbao.multiverse.enums.WeatherEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import java.util.List;
import java.util.Map;

/**
 * 宇宙市场气象（设计 §3.3.5 / §5.2）。
 * 多信号融合判断晴/多云/雨/风暴：
 * 搜索趋势=晴雨表（暂无时序信号，用竞争密度代理）、评论情绪=湿度、价格波动=气压、政策=锋面。
 * 规则化可解释，输出 7/30/90 天天气预报。
 */
@Component
public class WeatherForecaster {
    private static final Logger log = LoggerFactory.getLogger(WeatherForecaster.class);

    @Resource
    private UniverseWeatherDAO universeWeatherDAO;

    public UniverseWeatherDO forecast(Long universeId, CollectedDataBO data, String traceId) {
        double sentiment = sentiment(data);
        int competitorCount = listOf(data.getCompetitorData(), "competitors").size();
        int policyHigh = policyHigh(data);
        double priceSpread = priceSpread(data);

        WeatherEnum weather = fuse(sentiment, competitorCount, policyHigh);

        UniverseWeatherDO do_ = new UniverseWeatherDO();
        do_.setUniverseId(universeId);
        do_.setWeather(weather.getLabel());
        do_.setSearchSignal("竞争热度=竞品" + competitorCount + "家（搜索趋势时序信号待 P3 接入）");
        do_.setSentimentSignal("评论情绪=" + String.format("%.2f", sentiment));
        do_.setPriceSignal("价格波动=±" + Math.round(priceSpread * 100) + "%");
        do_.setPolicySignal("政策高风险=" + policyHigh + " 条");
        do_.setForecast7d(weather.getLabel());
        do_.setForecast30d(weather.getLabel());
        do_.setForecast90d(policyHigh >= 1 ? WeatherEnum.STORM.getLabel() : weather.getLabel());
        do_.setTraceId(traceId);
        log.info("天气预测 universeId={} weather={} sentiment={} competitors={} policyHigh={}",
                universeId, weather.getLabel(), sentiment, competitorCount, policyHigh);
        // 落库（此前 forecast 只构造 DO 从不 insert，导致 universe_weather 恒空）
        universeWeatherDAO.insert(do_);
        return do_;
    }

    /** 融合规则：政策高风险→风暴；情绪差或竞争过热→雨；情绪好且竞争低→晴；其余→多云 */
    private WeatherEnum fuse(double sentiment, int competitorCount, int policyHigh) {
        if (policyHigh >= 1) return WeatherEnum.STORM;
        if (sentiment < 0.4 || competitorCount > 8) return WeatherEnum.RAIN;
        if (sentiment > 0.7 && competitorCount <= 4) return WeatherEnum.SUNNY;
        return WeatherEnum.CLOUDY;
    }

    private double sentiment(CollectedDataBO data) {
        Object s = data.getReviewData() == null ? null : data.getReviewData().get("sentiment");
        return s instanceof Number ? ((Number) s).doubleValue() : 0.7;
    }

    private int policyHigh(CollectedDataBO data) {
        int high = 0;
        for (Map<String, Object> item : listOf(data.getComplianceData(), "compliance")) {
            if ("high".equalsIgnoreCase(String.valueOf(item.get("level")))) high++;
        }
        return high;
    }

    /** 竞品价格标准差 / 均价，作为价格波动（气压）信号 */
    private double priceSpread(CollectedDataBO data) {
        List<Map<String, Object>> competitors = listOf(data.getCompetitorData(), "competitors");
        if (competitors.isEmpty()) return 0;
        List<Double> prices = competitors.stream()
                .map(c -> c.get("price"))
                .filter(p -> p instanceof Number)
                .map(p -> ((Number) p).doubleValue())
                .filter(p -> p > 0)
                .toList();
        if (prices.size() < 2) return 0;
        double avg = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        if (avg <= 0) return 0;
        double variance = prices.stream().mapToDouble(p -> Math.pow(p - avg, 2)).average().orElse(0);
        return Math.sqrt(variance) / avg;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOf(Map<String, Object> data, String key) {
        if (data == null) return List.of();
        Object v = data.get(key);
        return v instanceof List ? (List<Map<String, Object>>) v : List.of();
    }
}
