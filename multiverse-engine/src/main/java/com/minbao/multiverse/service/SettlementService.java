package com.minbao.multiverse.service;

import com.minbao.multiverse.domain.dto.SubmitDecisionDTO;
import com.minbao.multiverse.domain.vo.DecisionVO;

public interface SettlementService {
    DecisionVO submitDecision(SubmitDecisionDTO dto);
    DecisionVO getDecisionByTaskId(Long taskId);
}