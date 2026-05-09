package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * CDN节点负载上报请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdnNodeReportDTO {
    
    /**
     * 节点标识符
     */
    private String nodeId;
    
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
     * 额外数据
     */
    private Map<String, Object> extraData;
}
