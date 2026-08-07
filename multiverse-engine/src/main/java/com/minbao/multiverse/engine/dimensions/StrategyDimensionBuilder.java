package com.minbao.multiverse.engine.dimensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StrategyDimensionBuilder {
    private static final Logger log = LoggerFactory.getLogger(StrategyDimensionBuilder.class);

    public void build() {
        log.info("构建策略维度");
    }
}
