package com.minbao.multiverse.service;

import com.minbao.multiverse.domain.dto.ExploreDTO;
import com.minbao.multiverse.domain.vo.UniverseVO;

public interface ExplorationService {
    UniverseVO explore(Long universeId, ExploreDTO dto);
    UniverseVO getUniverseDetail(Long universeId);
}