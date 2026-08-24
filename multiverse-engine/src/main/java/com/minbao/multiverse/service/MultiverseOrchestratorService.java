package com.minbao.multiverse.service;

import com.minbao.multiverse.domain.dto.CreateTaskDTO;
import com.minbao.multiverse.domain.dto.RetryTaskDTO;
import com.minbao.multiverse.domain.vo.CollectedDataVO;
import com.minbao.multiverse.domain.vo.ProgressVO;
import com.minbao.multiverse.domain.vo.TaskVO;
import com.minbao.multiverse.domain.vo.UniverseVO;

import java.util.List;

public interface MultiverseOrchestratorService {
    TaskVO createTask(CreateTaskDTO dto);
    TaskVO retryTask(Long taskId, RetryTaskDTO dto);
    TaskVO getTaskDetail(Long taskId);
    ProgressVO getProgress(Long taskId);
    List<UniverseVO> getUniversesByTaskId(Long taskId);
    /** P3：数据采集效果展示（market_data 持久化的来源 + 新鲜度） */
    CollectedDataVO getCollectedData(Long taskId);
}