package com.minbao.multiverse.manager.impl;

import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.EvolutionResultBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.manager.RuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class RuleEngineImpl implements RuleEngine {
    private static final Logger log = LoggerFactory.getLogger(RuleEngineImpl.class);

    @Override
    public EvolutionResultBO evolve(UniverseBO universe, CollectedDataBO data) {
        log.info("规则引擎推演 universeId={}", universe.getUniverseId());
        EvolutionResultBO result = new EvolutionResultBO();
        result.setEvidences(new ArrayList<>());
        result.setSurvivalRate(0.85);
        result.setRating("A");
        return result;
    }
}
