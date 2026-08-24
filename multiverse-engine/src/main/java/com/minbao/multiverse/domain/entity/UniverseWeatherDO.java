package com.minbao.multiverse.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 宇宙市场气象（设计 §3.3.5 / §5.2）。
 * 多信号融合（搜索趋势=晴雨表、评论情绪=湿度、价格波动=气压、政策=锋面）判断晴/多云/雨/风暴，
 * 并输出 7/30/90 天天气预报。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UniverseWeatherDO extends BaseDO {
    private Long universeId;
    private String weather;
    private String searchSignal;
    private String sentimentSignal;
    private String priceSignal;
    private String policySignal;
    private String forecast7d;
    private String forecast30d;
    private String forecast90d;
}
