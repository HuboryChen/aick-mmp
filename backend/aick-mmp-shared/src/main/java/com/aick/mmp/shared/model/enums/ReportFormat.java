package com.aick.mmp.shared.model.enums;

import lombok.Getter;

/**
 * 报表格式枚举
 */
@Getter
public enum ReportFormat {
    PDF("pdf", "PDF文档"),
    EXCEL("excel", "Excel表格"),
    CSV("csv", "CSV文件"),
    HTML("html", "HTML页面");

    private final String code;
    private final String description;

    ReportFormat(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ReportFormat fromCode(String code) {
        for (ReportFormat format : values()) {
            if (format.getCode().equals(code)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown report format: " + code);
    }
}
