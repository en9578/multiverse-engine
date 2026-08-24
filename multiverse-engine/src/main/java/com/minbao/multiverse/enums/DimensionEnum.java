package com.minbao.multiverse.enums;

import lombok.Getter;

/**
 * 宇宙维度（设计 §3.4）。
 * 时间维度产出 3 个时间宇宙（时间差机会卡），策略维度产出 5 个策略宇宙；
 * 关联/极端/天气三维度附着在策略宇宙之上，不单独成宇宙。
 */
@Getter
public enum DimensionEnum {
    TIME("TIME", "时间维度"),
    STRATEGY("STRATEGY", "策略维度");

    private final String code;
    private final String desc;

    DimensionEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
