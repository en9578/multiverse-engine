package com.minbao.multiverse.enums;

import lombok.Getter;

@Getter
public enum UniverseRatingEnum {
    A("优秀", 90, 100),
    B("良好", 75, 89),
    C("一般", 60, 74),
    D("较差", 40, 59),
    F("极差", 0, 39);

    private final String desc;
    private final int scoreMin;
    private final int scoreMax;

    UniverseRatingEnum(String desc, int scoreMin, int scoreMax) {
        this.desc = desc;
        this.scoreMin = scoreMin;
        this.scoreMax = scoreMax;
    }
}
