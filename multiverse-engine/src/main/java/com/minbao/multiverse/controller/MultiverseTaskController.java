package com.minbao.multiverse.controller;

import com.minbao.multiverse.common.Result;
import com.minbao.multiverse.domain.dto.CreateTaskDTO;
import com.minbao.multiverse.domain.dto.RetryTaskDTO;
import com.minbao.multiverse.domain.vo.CollectedDataVO;
import com.minbao.multiverse.domain.vo.ProgressVO;
import com.minbao.multiverse.domain.vo.TaskVO;
import com.minbao.multiverse.domain.vo.UniverseVO;
import com.minbao.multiverse.service.MultiverseOrchestratorService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class MultiverseTaskController {
    private static final Logger log = LoggerFactory.getLogger(MultiverseTaskController.class);

    @Resource
    private MultiverseOrchestratorService orchestratorService;

    @PostMapping
    public Result<TaskVO> createTask(@Valid @RequestBody CreateTaskDTO dto) {
        log.info("创建任务 productName={}", dto.getProductName());
        return Result.ok(orchestratorService.createTask(dto));
    }

    @GetMapping("/{taskId}")
    public Result<TaskVO> getTaskDetail(@PathVariable Long taskId) {
        return Result.ok(orchestratorService.getTaskDetail(taskId));
    }

    @GetMapping("/{taskId}/progress")
    public Result<ProgressVO> getProgress(@PathVariable Long taskId) {
        return Result.ok(orchestratorService.getProgress(taskId));
    }

    @GetMapping(value = "/{taskId}/stream", produces = "text/event-stream")
    public SseEmitter streamProgress(@PathVariable Long taskId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        try {
            emitter.send(SseEmitter.event().name("progress").data("{\"taskId\":" + taskId + ",\"stage\":\"CONNECTED\"}"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @PostMapping("/{taskId}/retry")
    public Result<TaskVO> retryTask(@PathVariable Long taskId, @Valid @RequestBody RetryTaskDTO dto) {
        log.info("重试任务 taskId={}", taskId);
        return Result.ok(orchestratorService.retryTask(taskId, dto));
    }

    @GetMapping("/{taskId}/universes")
    public Result<List<UniverseVO>> getUniverses(@PathVariable Long taskId) {
        return Result.ok(orchestratorService.getUniversesByTaskId(taskId));
    }

    /** P3：数据采集效果展示（来源 + last_verified + Fresh/Stale/Missing） */
    @GetMapping("/{taskId}/collected-data")
    public Result<CollectedDataVO> getCollectedData(@PathVariable Long taskId) {
        return Result.ok(orchestratorService.getCollectedData(taskId));
    }
}