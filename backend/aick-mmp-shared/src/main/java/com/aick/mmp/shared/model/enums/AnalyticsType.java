package com.aick.mmp.shared.model.enums;

import lombok.Getter;

/**
 * 统计类型枚举
 */
@Getter
public enum AnalyticsType {
    DEVICE_USAGE("device_usage", "设备利用率"),
    NETWORK_BANDWIDTH("network_bandwidth", "网络带宽"),
    STORAGE_CAPACITY("storage_capacity", "存储容量"),
    ALERT_COUNT("alert_count", "告警数量"),
    STREAM_QUALITY("stream_quality", "流质量"),
    USER_ACTIVITY("user_activity", "用户活动"),
    API_USAGE("api_usage", "API使用量"),
    COST_ANALYSIS("cost_analysis", "成本分析");

    private final String code;
    private final String description;

    AnalyticsType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AnalyticsType fromCode(String code) {
        for (AnalyticsType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown analytics type: " + code);
    }
}
