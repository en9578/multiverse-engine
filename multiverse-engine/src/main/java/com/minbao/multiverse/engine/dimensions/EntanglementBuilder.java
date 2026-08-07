package com.minbao.multiverse.engine.dimensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EntanglementBuilder {
    private static final Logger log = LoggerFactory.getLogger(EntanglementBuilder.class);

    public void build() {
        log.info("构建关联维度");
    }
}
