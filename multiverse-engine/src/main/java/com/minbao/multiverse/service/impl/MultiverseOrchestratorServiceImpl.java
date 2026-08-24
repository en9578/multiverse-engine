package com.minbao.multiverse.service.impl;

import com.minbao.multiverse.common.BusinessException;
import com.minbao.multiverse.common.JsonUtil;
import com.minbao.multiverse.dao.MultiverseTaskDAO;
import com.minbao.multiverse.dao.UniverseDAO;
import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.SettlementBO;
import com.minbao.multiverse.domain.dto.CreateTaskDTO;
import com.minbao.multiverse.domain.dto.RetryTaskDTO;
import com.minbao.multiverse.domain.entity.MultiverseTaskDO;
import com.minbao.multiverse.domain.entity.UniverseDO;
import com.minbao.multiverse.domain.vo.ProgressVO;
import com.minbao.multiverse.domain.vo.TaskVO;
import com.minbao.multiverse.domain.vo.UniverseVO;
import com.minbao.multiverse.enums.ErrorCodeEnum;
import com.minbao.multiverse.enums.TaskStatusEnum;
import com.minbao.multiverse.manager.MultiverseEngine;
import com.minbao.multiverse.service.MultiverseOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Service
public class MultiverseOrchestratorServiceImpl implements MultiverseOrchestratorService {
    private static final Logger log = LoggerFactory.getLogger(MultiverseOrchestratorServiceImpl.class);

    @Resource private MultiverseTaskDAO multiverseTaskDAO;
    @Resource private UniverseDAO universeDAO;
    @Resource private MultiverseEngine multiverseEngine;
    @Resource @Qualifier("multiverseExecutor") private ThreadPoolTaskExecutor multiverseExecutor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskVO createTask(CreateTaskDTO dto) {
        String requestId = (dto.getRequestId() == null || dto.getRequestId().isBlank())
                ? UUID.randomUUID().toString() : dto.getRequestId();

        MultiverseTaskDO existing = multiverseTaskDAO.selectByRequestId(requestId);
        if (existing != null) {
            log.info("幂等命中 requestId={}", requestId);
            return toTaskVO(existing);
        }

        MultiverseTaskDO task = new MultiverseTaskDO();
        task.setRequestId(requestId);
        task.setProductName(dto.getProductName());
        task.setTargetMarket(dto.getTargetMarket());
        task.setStrategyDesc(dto.getStrategyDesc() == null ? "" : dto.getStrategyDesc());
        task.setStatus(TaskStatusEnum.CREATED);
        task.setLastCompletedStage("");
        task.setTraceId(UUID.randomUUID().toString());
        task.setOverallProgress(0);
        task.setGmtCreate(LocalDateTime.now());
        task.setGmtModified(LocalDateTime.now());
        multiverseTaskDAO.insert(task);

        CompletableFuture.runAsync(() -> executeOrchestration(task), multiverseExecutor);
        return toTaskVO(task);
    }

    public void executeOrchestration(MultiverseTaskDO task) {
        try {
            updateStatus(task.getId(), TaskStatusEnum.COLLECTING);
            CollectedDataBO data = multiverseEngine.collectData(task);
            markStage(task.getId(), "COLLECTING", 20);

            updateStatus(task.getId(), TaskStatusEnum.GENERATING);
            multiverseEngine.generateUniverses(task, data);
            markStage(task.getId(), "GENERATING", 40);

            updateStatus(task.getId(), TaskStatusEnum.EXPLORING);
            multiverseEngine.exploreUniverses(task, data);
            markStage(task.getId(), "EXPLORING", 60);

            updateStatus(task.getId(), TaskStatusEnum.SETTLING);
            SettlementBO settlement = multiverseEngine.settle(task);
            multiverseTaskDAO.updateResult(task.getId(), JsonUtil.toJson(settlement), "SETTLING", 100);

            updateStatus(task.getId(), TaskStatusEnum.DONE);
            log.info("任务完成 taskId={}", task.getId());
        } catch (Exception e) {
            log.error("编排异常 taskId={}", task.getId(), e);
            updateStatus(task.getId(), TaskStatusEnum.FAILED);
        }
    }

    private void updateStatus(Long taskId, TaskStatusEnum status) {
        multiverseTaskDAO.updateStatus(taskId, status.name());
    }

    /** 阶段完成打 checkpoint（last_completed_stage + progress），用于断点恢复与进度展示 */
    private void markStage(Long taskId, String stage, int progress) {
        multiverseTaskDAO.updateResult(taskId, null, stage, progress);
    }

    @Override
    public TaskVO retryTask(Long taskId, RetryTaskDTO dto) {
        MultiverseTaskDO task = multiverseTaskDAO.selectById(taskId);
        if (task == null) throw new BusinessException(ErrorCodeEnum.TASK_NOT_FOUND);
        updateStatus(task.getId(), TaskStatusEnum.COLLECTING);
        CompletableFuture.runAsync(() -> executeOrchestration(task), multiverseExecutor);
        return toTaskVO(task);
    }

    @Override
    public TaskVO getTaskDetail(Long taskId) {
        MultiverseTaskDO task = multiverseTaskDAO.selectById(taskId);
        if (task == null) throw new BusinessException(ErrorCodeEnum.TASK_NOT_FOUND);
        return toTaskVO(task);
    }

    @Override
    public ProgressVO getProgress(Long taskId) {
        MultiverseTaskDO task = multiverseTaskDAO.selectById(taskId);
        if (task == null) throw new BusinessException(ErrorCodeEnum.TASK_NOT_FOUND);
        ProgressVO vo = new ProgressVO();
        vo.setTaskId(task.getId());
        vo.setStatus(task.getStatus());
        vo.setOverallProgress(calcProgress(task));
        vo.setCurrentStage(task.getStatus().name());
        return vo;
    }

    @Override
    public List<UniverseVO> getUniversesByTaskId(Long taskId) {
        List<UniverseDO> universes = universeDAO.selectByTaskId(taskId);
        return universes.stream().map(this::toUniverseVO).collect(Collectors.toList());
    }

    private int calcProgress(MultiverseTaskDO task) {
        return switch (task.getStatus()) {
            case CREATED -> 0;
            case COLLECTING -> 20;
            case GENERATING -> 40;
            case EXPLORING -> 60;
            case SETTLING -> 80;
            case DONE -> 100;
            case FAILED -> -1;
        };
    }

    private TaskVO toTaskVO(MultiverseTaskDO task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setRequestId(task.getRequestId());
        vo.setStatus(task.getStatus());
        vo.setOverallProgress(calcProgress(task));
        vo.setProductName(task.getProductName());
        vo.setTargetMarket(task.getTargetMarket());
        vo.setGmtCreate(task.getGmtCreate());
        return vo;
    }

    private UniverseVO toUniverseVO(UniverseDO universe) {
        UniverseVO vo = new UniverseVO();
        vo.setId(universe.getId());
        vo.setTaskId(universe.getTaskId());
        vo.setUniverseIndex(universe.getUniverseIndex());
        vo.setDimension(universe.getDimension());
        vo.setRating(universe.getRating());
        vo.setSubState(universe.getSubState());
        vo.setSurvivalRate(universe.getSurvivalRate());
        vo.setStrategyPackage(universe.getStrategyPackage());
        return vo;
    }
}