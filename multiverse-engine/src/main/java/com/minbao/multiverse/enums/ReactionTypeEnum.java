package com.minbao.multiverse.enums;

import lombok.Getter;

/**
 * 竞品关联反应模式（设计 §3.3.3）。
 * 卖家在某宇宙采用策略 X 后，竞品的反应：跟价/跟款/差异化/无视。
 */
@Getter
public enum ReactionTypeEnum {
    FOLLOW_PRICE("跟价"),
    FOLLOW_PRODUCT("跟款"),
    DIFFERENTIATE("差异化"),
    IGNORE("无视");

    private final String label;

    ReactionTypeEnum(String label) {
        this.label = label;
    }
}
