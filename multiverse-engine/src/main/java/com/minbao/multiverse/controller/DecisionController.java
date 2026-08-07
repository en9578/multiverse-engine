package com.minbao.multiverse.controller;

import com.minbao.multiverse.common.Result;
import com.minbao.multiverse.domain.dto.SubmitDecisionDTO;
import com.minbao.multiverse.domain.vo.DecisionVO;
import com.minbao.multiverse.service.SettlementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/api/v1/decisions")
public class DecisionController {
    private static final Logger log = LoggerFactory.getLogger(DecisionController.class);

    @Resource
    private SettlementService settlementService;

    @PostMapping
    public Result<DecisionVO> submitDecision(@Valid @RequestBody SubmitDecisionDTO dto) {
        log.info("提交定居决策 taskId={} universeId={}", dto.getTaskId(), dto.getUniverseId());
        return Result.ok(settlementService.submitDecision(dto));
    }

    @GetMapping("/{taskId}")
    public Result<DecisionVO> getDecision(@PathVariable Long taskId) {
        log.info("查询决策结果 taskId={}", taskId);
        return Result.ok(settlementService.getDecisionByTaskId(taskId));
    }
}