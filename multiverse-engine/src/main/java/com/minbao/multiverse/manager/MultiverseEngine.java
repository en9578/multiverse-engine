package com.minbao.multiverse.manager;

import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.SettlementBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.domain.entity.MultiverseTaskDO;

import java.util.List;

public interface MultiverseEngine {
    void collectData(MultiverseTaskDO task);
    List<UniverseBO> generateUniverses(MultiverseTaskDO task);
    void exploreUniverses(MultiverseTaskDO task);
    SettlementBO settle(MultiverseTaskDO task);
}
