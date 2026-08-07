package com.minbao.multiverse.manager.impl;

import com.minbao.multiverse.dao.MultiverseTaskDAO;
import com.minbao.multiverse.domain.bo.CollectedDataBO;
import com.minbao.multiverse.domain.bo.SettlementBO;
import com.minbao.multiverse.domain.bo.UniverseBO;
import com.minbao.multiverse.domain.entity.MultiverseTaskDO;
import com.minbao.multiverse.manager.BailianManager;
import com.minbao.multiverse.manager.MultiverseEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Service
public class MultiverseEngineImpl implements MultiverseEngine {
    private static final Logger log = LoggerFactory.getLogger(MultiverseEngineImpl.class);

    @Resource private MultiverseTaskDAO multiverseTaskDAO;
    @Resource private BailianManager bailianManager;

    @Override
    public void collectData(MultiverseTaskDO task) {
        log.info("收集数据 taskId={}", task.getId());
        // 骨架：实际调用百炼做评论基因检测
    }

    @Override
    public List<UniverseBO> generateUniverses(MultiverseTaskDO task) {
        log.info("生成宇宙 taskId={}", task.getId());
        return Collections.singletonList(new UniverseBO());
    }

    @Override
    public void exploreUniverses(MultiverseTaskDO task) {
        log.info("探索宇宙 taskId={}", task.getId());
    }

    @Override
    public SettlementBO settle(MultiverseTaskDO task) {
        log.info("结算 taskId={}", task.getId());
        return new SettlementBO();
    }
}
