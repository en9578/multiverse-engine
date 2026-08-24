package com.minbao.multiverse.service.impl;

import com.minbao.multiverse.common.BusinessException;
import com.minbao.multiverse.common.JsonUtil;
import com.minbao.multiverse.dao.SettlementDecisionDAO;
import com.minbao.multiverse.dao.UniverseDAO;
import com.minbao.multiverse.domain.dto.SubmitDecisionDTO;
import com.minbao.multiverse.domain.entity.SettlementDecisionDO;
import com.minbao.multiverse.domain.entity.UniverseDO;
import com.minbao.multiverse.domain.vo.DecisionVO;
import com.minbao.multiverse.enums.ErrorCodeEnum;
import com.minbao.multiverse.service.SettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

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
        UniverseDO universe = universeDAO.selectById(dto.getUniverseId());
        if (universe == null || !universe.getTaskId().equals(dto.getTaskId())) {
            throw new BusinessException(ErrorCodeEnum.UNIVERSE_NOT_FOUND);
        }

        // 已有决策（编排阶段自动生成的反脆弱组合）→ 人工确认；无决策（手动选择其他宇宙）→ 补一条已确认决策
        List<SettlementDecisionDO> decisions = settlementDecisionDAO.selectByUniverseId(dto.getUniverseId());
        SettlementDecisionDO decision;
        if (!decisions.isEmpty()) {
            decision = decisions.get(0);
            settlementDecisionDAO.updateConfirm(decision.getId(), true);
            log.info("确认既有决策 decisionId={}", decision.getId());
        } else {
            decision = new SettlementDecisionDO();
            decision.setUniverseId(dto.getUniverseId());
            decision.setDecisionData(JsonUtil.toJson(Map.of(
                    "selectedUniverseId", dto.getUniverseId(),
                    "source", "manual_confirm",
                    "strategyPackage", universe.getStrategyPackage() == null ? "" : universe.getStrategyPackage())));
            decision.setIsConfirmed(true);
            decision.setTraceId(universe.getTraceId());
            settlementDecisionDAO.insert(decision);
            log.info("补录手动确认决策 universeId={}", dto.getUniverseId());
        }

        DecisionVO vo = new DecisionVO();
        vo.setTaskId(dto.getTaskId());
        vo.setUniverseId(dto.getUniverseId());
        vo.setDecisionData(decision.getDecisionData());
        vo.setIsConfirmed(true);
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