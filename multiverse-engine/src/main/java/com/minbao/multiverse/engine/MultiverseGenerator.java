package com.minbao.multiverse.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MultiverseGenerator {
    private static final Logger log = LoggerFactory.getLogger(MultiverseGenerator.class);

    public void generate() {
        log.info("生成宇宙");
    }
}
