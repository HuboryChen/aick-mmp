package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CDN节点数据传输对象
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CdnNodeDTO {
    
    private Long id;
    
    /**
     * 节点唯一标识符
     */
    private String nodeId;
    
    /**
     * 节点名称
     */
    private String name;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 端口
     */
    private Integer port;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 地理位置（详细地址）
     */
    private String location;
    
    /**
     * 关联区域ID
     */
    private Long regionId;
    
    /**
     * 关联区域名称（用于显示）
     */
    private String regionName;
    
    /**
     * 容量
     */
    private Integer capacity;
    
    /**
     * 当前负载
     */
    private Integer currentLoad;
    
    /**
     * 最后心跳时间
     */
    private String lastHeartbeat;
    
    /**
     * CPU使用率
     */
    private Double cpuUsage;
    
    /**
     * 内存使用率
     */
    private Double memoryUsage;
    
    /**
     * 带宽使用率
     */
    private Double bandwidthUsage;
    
    /**
     * 存储使用率
     */
    private Double storageUsage;
    
    /**
     * 上行带宽（Mbps）
     */
    private Integer upBandwidth;
    
    /**
     * 下行带宽（Mbps）
     */
    private Integer downBandwidth;
    
    /**
     * 权重
     */
    private Integer weight;
    
    /**
     * 优先级
     */
    private Integer priority;
    
    /**
     * 健康检查URL
     */
    private String healthCheckUrl;
    
    /**
     * 是否启用
     */
    private Boolean isEnabled;
    
    /**
     * 负载百分比
     */
    private Double loadPercentage;
    
    /**
     * WLC权重分数
     */
    private Double wlcScore;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
