package com.minbao.multiverse.domain.vo;

import lombok.Data;

/**
 * 单条极端风暴压力测试结果（穿越体验舱 5 风暴雷达图数据源）。
 */
@Data
public class StressTestVO {
    /** 风暴中文名，如「价格海啸」（落库即中文 label，雷达图按返回顺序渲染） */
    private String storm;
    /** 该风暴下存活率 0-1 */
    private Double survivalRate;
    private String weakestLink;
    private String fixSuggestion;
}
