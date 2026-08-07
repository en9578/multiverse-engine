package com.minbao.multiverse.engine.dimensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExtremeDimensionBuilder {
    private static final Logger log = LoggerFactory.getLogger(ExtremeDimensionBuilder.class);

    public void build() {
        log.info("构建极端维度");
    }
}
