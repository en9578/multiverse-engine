package com.minbao.multiverse.engine.evolution;

import com.minbao.multiverse.enums.StageEnum;
import com.minbao.multiverse.manager.BailianManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * R1 推理增强（设计 §3.3.3 / §4.2）。
 * 规则引擎做可解释核心推演，R1（deepseek-v4-pro）在规则未覆盖的场景做补充，
 * 补充结果标注 source: "r1_inferred"（半权重，未经知识库验证）。
 * 调用失败时返回 null，由调用方降级为「仅规则推演」，不中断编排。
 */
@Component
public class R1Enhancer {
    private static final Logger log = LoggerFactory.getLogger(R1Enhancer.class);

    @Resource
    private BailianManager bailianManager;

    /**
     * R1 推理增强：按给定提示词做深度推理，返回原始输出（由调用方解析）。
     * 失败返回 null。
     */
    public String enhance(String systemPrompt, String userPrompt) {
        try {
            return bailianManager.generateText(StageEnum.EXPLORING, systemPrompt, userPrompt);
        } catch (Exception e) {
            log.warn("R1 增强失败，降级为仅规则推演：{}", e.getMessage());
            return null;
        }
    }
}
