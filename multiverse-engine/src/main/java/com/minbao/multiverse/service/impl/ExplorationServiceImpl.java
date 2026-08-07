package com.minbao.multiverse.service.impl;

import com.minbao.multiverse.dao.UniverseDAO;
import com.minbao.multiverse.domain.dto.ExploreDTO;
import com.minbao.multiverse.domain.entity.UniverseDO;
import com.minbao.multiverse.domain.vo.UniverseVO;
import com.minbao.multiverse.service.ExplorationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class ExplorationServiceImpl implements ExplorationService {
    private static final Logger log = LoggerFactory.getLogger(ExplorationServiceImpl.class);

    @Resource
    private UniverseDAO universeDAO;

    @Override
    public UniverseVO explore(Long universeId, ExploreDTO dto) {
        log.info("穿梭体验 universeId={} sessionId={}", universeId, dto.getSessionId());
        // 骨架：实际应调用 Python 引擎做对话式探索
        return getUniverseDetail(universeId);
    }

    @Override
    public UniverseVO getUniverseDetail(Long universeId) {
        UniverseDO universe = universeDAO.selectById(universeId);
        if (universe == null) {
            return null;
        }
        UniverseVO vo = new UniverseVO();
        vo.setId(universe.getId());
        vo.setTaskId(universe.getTaskId());
        vo.setUniverseIndex(universe.getUniverseIndex());
        vo.setRating(universe.getRating());
        vo.setSubState(universe.getSubState());
        vo.setSurvivalRate(universe.getSurvivalRate());
        vo.setStrategyPackage(universe.getStrategyPackage());
        return vo;
    }
}