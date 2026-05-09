package com.aick.mmp.central.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 边缘节点故障转移配置
 * 绑定 edge.failover.* 前缀的配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "edge.failover")
public class EdgeFailoverProperties {

    /**
     * 是否启用自动故障转移（节点离线时自动迁移摄像头）
     */
    private boolean enabled = true;

    /**
     * 触发模式：sync（同步）或 async（异步）
     */
    private FailoverMode mode = FailoverMode.ASYNC;

    /**
     * 触发后延迟执行的秒数（用于等待节点可能的短暂恢复）
     * 0 表示不延迟，立即执行
     */
    private int delaySeconds = 0;

    /**
     * 最大并发故障转移任务数
     * 防止多节点同时离线时产生大量迁移操作
     */
    private int maxConcurrentTasks = 3;

    /**
     * 每批处理的摄像头数量
     */
    private int batchSize = 20;

    /**
     * 批次间的延迟毫秒数
     */
    private long batchDelayMs = 1000L;

    /**
     * 同区域节点的评分加成比例（0.0 - 1.0）
     * 例如 0.3 表示同区域节点获得30%的额外权重
     */
    private double regionBonus = 0.3;

    /**
     * 待分配池重试分配间隔秒数
     */
    private int retryIntervalSeconds = 300;

    public enum FailoverMode {
        SYNC,
        ASYNC
    }
}
