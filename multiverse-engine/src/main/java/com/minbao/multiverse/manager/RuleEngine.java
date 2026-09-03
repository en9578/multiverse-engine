package com.minbao.multiverse.manager;

import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.EvolutionResultBO;
import com.minbao.multiverse.domain.bo.UniverseBO;

public interface RuleEngine {
    EvolutionResultBO evolve(UniverseBO universe, CollectedDataBO data);

    /**
     * 策略画像先验分（0-100）：市场事实缺失（无 LLM）时，按策略组合
     * pricing×sellingPoint×positioning 查启发式先验表，为 5 个策略宇宙提供确定性区分。
     * 仅用于降级融合分支；有真实 LLM 时不影响评分。
     */
    double strategyPrior(UniverseBO universe);
}
