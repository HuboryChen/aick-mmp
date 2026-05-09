package com.aick.mmp.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * CDN节点负载历史记录实体
 * 用于存储CDN节点的负载历史数据，用于分析和报表
 */
@Entity
@Table(name = "cdn_node_load_history", indexes = {
    @Index(name = "idx_cdn_node_load_node_id", columnList = "cdn_node_id"),
    @Index(name = "idx_cdn_node_load_recorded_at", columnList = "recorded_at"),
    @Index(name = "idx_cdn_node_load_node_time", columnList = "cdn_node_id, recorded_at")
})
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CdnNodeLoad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的CDN节点ID
     */
    @Column(name = "cdn_node_id", nullable = false)
    private Long cdnNodeId;

    /**
     * 记录时间
     */
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    /**
     * 当前负载（连接数/请求数）
     */
    @Column(name = "current_load")
    private Integer currentLoad;

    /**
     * CPU使用率（百分比）
     */
    @Column(name = "cpu_usage")
    private Double cpuUsage;

    /**
     * 内存使用率（百分比）
     */
    @Column(name = "memory_usage")
    private Double memoryUsage;

    /**
     * 带宽使用率（百分比）
     */
    @Column(name = "bandwidth_usage")
    private Double bandwidthUsage;

    /**
     * 存储使用率（百分比）
     */
    @Column(name = "storage_usage")
    private Double storageUsage;

    /**
     * 上行带宽（Mbps）
     */
    @Column(name = "up_bandwidth")
    private Integer upBandwidth;

    /**
     * 下行带宽（Mbps）
     */
    @Column(name = "down_bandwidth")
    private Integer downBandwidth;

    /**
     * 活跃连接数
     */
    @Column(name = "active_connections")
    private Integer activeConnections;

    /**
     * 请求速率（请求/秒）
     */
    @Column(name = "request_rate")
    private Double requestRate;

    /**
     * 带宽吞吐量（Mbps）
     */
    @Column(name = "bandwidth_throughput")
    private Double bandwidthThroughput;

    /**
     * 命中率（缓存命中率百分比）
     */
    @Column(name = "cache_hit_rate")
    private Double cacheHitRate;

    /**
     * 平均响应时间（毫秒）
     */
    @Column(name = "avg_response_time")
    private Double avgResponseTime;

    /**
     * 错误率（百分比）
     */
    @Column(name = "error_rate")
    private Double errorRate;

    /**
     * 节点状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CdnNode.NodeStatus status;

    /**
     * 负载百分比
     */
    @Column(name = "load_percentage")
    private Double loadPercentage;

    /**
     * 额外数据（JSON格式）
     */
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 创建记录时的时间戳
     */
    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }
}
