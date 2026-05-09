package com.aick.mmp.shared.model.enums;

import lombok.Getter;

/**
 * 数据聚合粒度枚举
 */
@Getter
public enum AggregationLevel {
    MINUTE("minute", "分钟"),
    HOUR("hour", "小时"),
    DAY("day", "日"),
    WEEK("week", "周"),
    MONTH("month", "月");

    private final String code;
    private final String description;

    AggregationLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AggregationLevel fromCode(String code) {
        for (AggregationLevel level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown aggregation level: " + code);
    }
}
