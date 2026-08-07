package com.minbao.multiverse.engine.evolution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StressTestEngine {
    private static final Logger log = LoggerFactory.getLogger(StressTestEngine.class);

    public void stressTest() {
        log.info("压力测试");
    }
}
