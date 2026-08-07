package com.minbao.multiverse.service.impl;

import com.minbao.multiverse.dao.SettlementDecisionDAO;
import com.minbao.multiverse.dao.UniverseDAO;
import com.minbao.multiverse.domain.dto.SubmitDecisionDTO;
import com.minbao.multiverse.domain.entity.SettlementDecisionDO;
import com.minbao.multiverse.domain.entity.UniverseDO;
import com.minbao.multiverse.domain.vo.DecisionVO;
import com.minbao.multiverse.service.SettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class SettlementServiceImpl implements SettlementService {
    private static final Logger log = LoggerFactory.getLogger(SettlementServiceImpl.class);

    @Resource
    private SettlementDecisionDAO settlementDecisionDAO;
    @Resource
    private UniverseDAO universeDAO;

    @Override
    public DecisionVO submitDecision(SubmitDecisionDTO dto) {
        log.info("提交定居决策 taskId={} universeId={}", dto.getTaskId(), dto.getUniverseId());
        // 骨架：实际应调用 Python 引擎做反脆弱组合计算后持久化
        DecisionVO vo = new DecisionVO();
        vo.setTaskId(dto.getTaskId());
        vo.setUniverseId(dto.getUniverseId());
        vo.setIsConfirmed(false);
        return vo;
    }

    @Override
    public DecisionVO getDecisionByTaskId(Long taskId) {
        log.info("查询决策结果 taskId={}", taskId);
        // 骨架：通过 taskId 查 universe 再查 decision
        List<UniverseDO> universes = universeDAO.selectByTaskId(taskId);
        for (UniverseDO universe : universes) {
            List<SettlementDecisionDO> decisions = settlementDecisionDAO.selectByUniverseId(universe.getId());
            if (!decisions.isEmpty()) {
                SettlementDecisionDO decision = decisions.get(0);
                DecisionVO vo = new DecisionVO();
                vo.setTaskId(taskId);
                vo.setUniverseId(decision.getUniverseId());
                vo.setDecisionData(decision.getDecisionData());
                vo.setIsConfirmed(decision.getIsConfirmed());
                return vo;
            }
        }
        return null;
    }
}