package com.aick.mmp.central.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "edge-node")
public class EdgeNodeProperties {

    /**
     * 同区域节点的评分加成比例（0.0 - 1.0）
     * 例如 0.3 表示同区域节点获得30%的额外权重
     */
    private double regionBonusRate = 0.3;

    /**
     * CPU 使用率阈值（百分比），超过此值视为不健康
     */
    private double cpuThreshold = 80.0;

    /**
     * 内存使用率阈值（百分比），超过此值视为不健康
     */
    private double memoryThreshold = 85.0;
}
