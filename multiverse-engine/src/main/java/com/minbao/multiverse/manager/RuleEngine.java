package com.minbao.multiverse.manager;

import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.EvolutionResultBO;
import com.minbao.multiverse.domain.bo.UniverseBO;

public interface RuleEngine {
    EvolutionResultBO evolve(UniverseBO universe, CollectedDataBO data);
}
