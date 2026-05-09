package com.aick.mmp.shared.model.enums;

import lombok.Getter;

/**
 * 报表类型枚举
 */
@Getter
public enum ReportType {
    DAILY("daily", "日报"),
    WEEKLY("weekly", "周报"),
    MONTHLY("monthly", "月报"),
    QUARTERLY("quarterly", "季报"),
    YEARLY("yearly", "年报"),
    CUSTOM("custom", "自定义");

    private final String code;
    private final String description;

    ReportType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ReportType fromCode(String code) {
        for (ReportType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown report type: " + code);
    }
}
