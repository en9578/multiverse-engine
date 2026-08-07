package com.minbao.multiverse.controller;

import com.minbao.multiverse.common.Result;
import com.minbao.multiverse.domain.dto.ExploreDTO;
import com.minbao.multiverse.domain.vo.UniverseVO;
import com.minbao.multiverse.service.ExplorationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/api/v1/universes")
public class UniverseController {
    private static final Logger log = LoggerFactory.getLogger(UniverseController.class);

    @Resource
    private ExplorationService explorationService;

    @GetMapping("/{universeId}")
    public Result<UniverseVO> getUniverseDetail(@PathVariable Long universeId) {
        log.info("查询宇宙详情 universeId={}", universeId);
        return Result.ok(explorationService.getUniverseDetail(universeId));
    }

    @PostMapping("/{universeId}/explore")
    public Result<UniverseVO> explore(@PathVariable Long universeId, @Valid @RequestBody ExploreDTO dto) {
        log.info("穿梭体验 universeId={}", universeId);
        return Result.ok(explorationService.explore(universeId, dto));
    }
}