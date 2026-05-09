package com.aick.mmp.shared.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 配置值类型枚举
 */
@Getter
@AllArgsConstructor
public enum ConfigValueType {
    
    STRING("字符串"),
    NUMBER("数字"),
    BOOLEAN("布尔值"),
    JSON("JSON对象"),
    TEXT("多行文本"),
    EMAIL("邮箱"),
    URL("URL地址"),
    IP("IP地址"),
    PORT("端口号"),
    SELECT("选择项"),
    MULTI_SELECT("多选");
    
    private final String label;
}
