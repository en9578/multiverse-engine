package com.minbao.multiverse.domain.vo;

import lombok.Data;

/**
 * 宇宙市场气象（穿越体验舱天气面板数据源）。
 */
@Data
public class UniverseWeatherVO {
    /** 晴/多云/雨/风暴（中文 label） */
    private String weather;
    private String searchSignal;
    private String sentimentSignal;
    private String priceSignal;
    private String policySignal;
    private String forecast7d;
    private String forecast30d;
    private String forecast90d;
}
