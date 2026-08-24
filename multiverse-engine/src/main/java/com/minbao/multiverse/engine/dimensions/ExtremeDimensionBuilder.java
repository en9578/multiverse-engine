package com.minbao.multiverse.engine.dimensions;

import com.minbao.multiverse.dao.StressTestDAO;
import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.domain.entity.StressTestDO;
import com.minbao.multiverse.engine.evolution.StressTestEngine;
import com.minbao.multiverse.enums.StormEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 极端维度宇宙（设计 §3.3.4 / §5.3）。
 * 每个策略宇宙被投入全部 5 种风暴压力测试，输出存活率 + 最弱环节 + 修复建议。
 * 由 StressTestEngine 规则化计算（可解释，非黑盒）。
 */
@Component
public class ExtremeDimensionBuilder {
    private static final Logger log = LoggerFactory.getLogger(ExtremeDimensionBuilder.class);

    @Resource private StressTestDAO stressTestDAO;
    @Resource private StressTestEngine stressTestEngine;

    public List<StressTestDO> build(UniverseBO universe, CollectedDataBO data, String traceId) {
        List<StressTestDO> results = new ArrayList<>();
        for (StormEnum storm : StormEnum.values()) {
            StressTestEngine.StormResult r = stressTestEngine.stressTest(universe, data, storm);
            StressTestDO test = new StressTestDO();
            test.setUniverseId(universe.getUniverseId());
            test.setStorm(storm.getLabel());
            test.setSurvivalRate(Math.round(r.survivalRate() * 100) / 100.0);
            test.setWeakestLink(r.weakestLink());
            test.setFixSuggestion(r.fix());
            test.setTraceId(traceId);
            stressTestDAO.insert(test);
            results.add(test);
        }
        log.info("极端压力测试完成 universeId={} storms={}", universe.getUniverseId(), results.size());
        return results;
    }
}
