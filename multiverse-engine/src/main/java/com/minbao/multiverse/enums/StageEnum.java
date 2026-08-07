package com.minbao.multiverse.enums;

public enum StageEnum {
    COMMENT_GENE("评论基因检测"),
    STRATEGY_VISUAL("策略可视化"),
    COMPLIANCE_CHECK("合规检测"),
    MARKET_EVOLVE("市场演化推演");

    private final String desc;
    StageEnum(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
