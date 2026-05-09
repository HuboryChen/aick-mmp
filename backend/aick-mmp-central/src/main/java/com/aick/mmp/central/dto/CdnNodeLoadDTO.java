package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CDN节点负载数据传输对象
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CdnNodeLoadDTO {
    
    private Long id;
    
    /**
     * 节点ID
     */
    private Long cdnNodeId;
    
    /**
     * 记录时间
     */
    private LocalDateTime recordedAt;
    
    /**
     * 当前负载
     */
    private Integer currentLoad;
    
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
     * 上行带宽
     */
    private Integer upBandwidth;
    
    /**
     * 下行带宽
     */
    private Integer downBandwidth;
    
    /**
     * 活跃连接数
     */
    private Integer activeConnections;
    
    /**
     * 请求速率
     */
    private Double requestRate;
    
    /**
     * 带宽吞吐量
     */
    private Double bandwidthThroughput;
    
    /**
     * 缓存命中率
     */
    private Double cacheHitRate;
    
    /**
     * 平均响应时间
     */
    private Double avgResponseTime;
    
    /**
     * 错误率
     */
    private Double errorRate;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 负载百分比
     */
    private Double loadPercentage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
