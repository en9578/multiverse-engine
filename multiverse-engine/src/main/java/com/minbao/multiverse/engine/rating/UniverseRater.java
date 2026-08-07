package com.minbao.multiverse.engine.rating;

import com.minbao.multiverse.enums.UniverseRatingEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UniverseRater {
    private static final Logger log = LoggerFactory.getLogger(UniverseRater.class);

    public UniverseRatingEnum rate(double score) {
        log.info("评级 score={}", score);
        if (score >= 90) return UniverseRatingEnum.A;
        if (score >= 75) return UniverseRatingEnum.B;
        if (score >= 60) return UniverseRatingEnum.C;
        if (score >= 40) return UniverseRatingEnum.D;
        return UniverseRatingEnum.F;
    }
}
