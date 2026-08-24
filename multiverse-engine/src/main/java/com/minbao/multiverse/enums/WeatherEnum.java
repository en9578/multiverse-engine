package com.minbao.multiverse.enums;

import lombok.Getter;

/**
 * 宇宙市场气象（设计 §3.3.5 / §5.2）。
 */
@Getter
public enum WeatherEnum {
    SUNNY("晴", "蓝海爆发"),
    CLOUDY("多云", "平稳竞争"),
    RAIN("雨", "红海竞争"),
    STORM("风暴", "极端事件");

    private final String label;
    private final String desc;

    WeatherEnum(String label, String desc) {
        this.label = label;
        this.desc = desc;
    }
}
