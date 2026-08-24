package com.minbao.multiverse.engine;

import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.domain.entity.MultiverseTaskDO;
import com.minbao.multiverse.engine.dimensions.EntanglementBuilder;
import com.minbao.multiverse.engine.dimensions.ExtremeDimensionBuilder;
import com.minbao.multiverse.engine.dimensions.StrategyDimensionBuilder;
import com.minbao.multiverse.engine.dimensions.TimeDimensionBuilder;
import com.minbao.multiverse.engine.dimensions.WeatherForecaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 多元宇宙生成器（设计 §3.4 generate_multiverse / §3.5 组件树）。
 * 编排：3 时间宇宙 + 5 策略宇宙（每个策略宇宙附加 关联/极端/天气 三维度）。
 */
@Component
public class MultiverseGenerator {
    private static final Logger log = LoggerFactory.getLogger(MultiverseGenerator.class);

    @Resource private TimeDimensionBuilder timeDimensionBuilder;
    @Resource private StrategyDimensionBuilder strategyDimensionBuilder;
    @Resource private EntanglementBuilder entanglementBuilder;
    @Resource private ExtremeDimensionBuilder extremeDimensionBuilder;
    @Resource private WeatherForecaster weatherForecaster;

    /** 多元宇宙结果：时间宇宙 + 策略宇宙（策略宇宙已附着关联反应/压力测试/天气） */
    public record Multiverse(List<UniverseBO> timeUniverses, List<UniverseBO> strategyUniverses) {}

    public Multiverse generate(MultiverseTaskDO task, CollectedDataBO data) {
        log.info("多元宇宙生成 taskId={}", task.getId());
        List<UniverseBO> timeUniverses = timeDimensionBuilder.buildUniverses(task, data);
        List<UniverseBO> strategyUniverses = strategyDimensionBuilder.buildUniverses(task, data);

        for (UniverseBO universe : strategyUniverses) {
            entanglementBuilder.build(universe, data, task.getTraceId());
            extremeDimensionBuilder.build(universe, data, task.getTraceId());
            weatherForecaster.forecast(universe.getUniverseId(), data, task.getTraceId());
        }
        log.info("多元宇宙生成完成 taskId={} time={} strategies={}",
                task.getId(), timeUniverses.size(), strategyUniverses.size());
        return new Multiverse(timeUniverses, strategyUniverses);
    }
}
