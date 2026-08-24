package com.minbao.multiverse.manager;

import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.SettlementBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.domain.entity.MultiverseTaskDO;

import java.util.List;

/**
 * 多元宇宙引擎：collect -> generate -> explore -> settle 四阶段。
 * 采集数据由 collectData 产出并在后续阶段间内存共享（采集 1 次，多宇宙共用）。
 */
public interface MultiverseEngine {
    CollectedDataBO collectData(MultiverseTaskDO task);

    List<UniverseBO> generateUniverses(MultiverseTaskDO task, CollectedDataBO data);

    void exploreUniverses(MultiverseTaskDO task, CollectedDataBO data);

    SettlementBO settle(MultiverseTaskDO task);
}
