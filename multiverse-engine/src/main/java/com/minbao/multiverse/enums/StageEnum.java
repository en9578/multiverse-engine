package com.minbao.multiverse.enums;

/**
 * 编排阶段枚举，与 TaskStatusEnum 状态机对应。
 * 每个阶段绑定了百炼文本模型路由（设计 §3.2 / §14.3），BailianManager 据此自动选择模型。
 * 生图 / 视觉 / 向量化等非文本模型由 BailianManager 内部常量独立管理，不经此路由。
 */
public enum StageEnum {
    /** 数据采集：竞品事实提取 / 合规检测 / 向量化（文本分析走 qwen3.7-plus） */
    COLLECTING("qwen3.7-plus", "数据采集"),
    /** 宇宙生成：5 宇宙策略包生成（复杂推理走 deepseek-v4-pro；配图走 wan2.7-image-pro 独立方法） */
    GENERATING("deepseek-v4-pro", "宇宙生成"),
    /** 格局推演：5 宇宙并行推演（复杂推理走 deepseek-v4-pro，交叉验证走 qwen3.8-max） */
    EXPLORING("deepseek-v4-pro", "格局推演"),
    /** 决策汇总：最优宇宙选择 + 反脆弱组合（交叉验证+决策走 qwen3.8-max） */
    SETTLING("qwen3.8-max", "决策汇总");

    private final String modelName;
    private final String desc;

    StageEnum(String modelName, String desc) {
        this.modelName = modelName;
        this.desc = desc;
    }

    public String getModelName() { return modelName; }
    public String getDesc() { return desc; }
}
