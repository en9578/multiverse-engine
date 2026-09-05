package com.minbao.multiverse.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 竞品关联反应（设计 §3.3.3）。
 * 每个策略宇宙推演竞品对该宇宙策略的反应：跟价/跟款/差异化/无视。
 * source 标注可解释性：heuristic=确定性阈值启发式（竞品价格/评分/销量判断），
 * r1_inferred=R1 推理增强补充；kb=历史反应模式（competitor_strategy_kb 反哺后启用，当前未用）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompetitorReactionDO extends BaseDO {
    private Long universeId;
    private String competitorName;
    private String reactionType;
    private Double probability;
    private String impact;
    private String source;
    private String evidence;
}
