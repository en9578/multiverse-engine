package com.minbao.multiverse.engine.dimensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WeatherForecaster {
    private static final Logger log = LoggerFactory.getLogger(WeatherForecaster.class);

    public void forecast() {
        log.info("市场天气预报");
    }
}
