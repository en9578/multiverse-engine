package com.minbao.multiverse.domain.vo;

import lombok.Data;

/**
 * 竞品对该宇宙策略的关联反应（跟价/跟款/差异化/无视）。
 */
@Data
public class CompetitorReactionVO {
    private String competitorName;
    private String reactionType;
    private Double probability;
    private String impact;
    /** 可解释来源：kb=规则引擎历史反应模式 / r1_inferred=R1 推理增强 */
    private String source;
    private String evidence;
}
