package com.aick.mmp.shared.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 配置分类枚举
 */
@Getter
@AllArgsConstructor
public enum ConfigCategory {
    
    // 视频参数
    VIDEO_QUALITY("视频参数", 1),
    VIDEO_STREAMING("视频流", 2),
    VIDEO_STORAGE("视频存储", 3),
    
    // 录像设置
    RECORDING_SCHEDULE("录像计划", 10),
    RECORDING_STORAGE("录像存储", 11),
    RECORDING_RETENTION("录像保留", 12),
    
    // 负载均衡
    LOAD_BALANCING("负载均衡", 20),
    CDN_NODES("CDN节点", 21),
    
    // 安全策略
    SECURITY_POLICY("安全策略", 30),
    AUTHENTICATION("认证配置", 31),
    API_KEYS("API密钥", 32),
    
    // 告警设置
    ALERT_SETTINGS("告警设置", 40),
    NOTIFICATION_CHANNELS("通知渠道", 41),
    
    // 边缘节点
    EDGE_NODES("边缘节点", 50),
    EDGE_FAILOVER("边缘故障转移", 51),
    
    // 系统参数
    SYSTEM_PARAMS("系统参数", 99);
    
    /**
     * 分类名称
     */
    private final String label;
    
    /**
     * 排序值
     */
    private final int sortOrder;
}
