package com.minbao.multiverse.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 极端风暴压力测试（设计 §3.3.4 / §5.3）。
 * 每个策略宇宙被投入 5 种风暴，输出存活率 + 最弱环节 + 修复建议。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StressTestDO extends BaseDO {
    private Long universeId;
    private String storm;
    private Double survivalRate;
    private String weakestLink;
    private String fixSuggestion;
}
