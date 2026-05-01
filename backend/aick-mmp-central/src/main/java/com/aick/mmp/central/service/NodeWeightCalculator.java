package com.aick.mmp.central.service;

import com.aick.mmp.shared.model.EdgeNode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 节点权重计算服务
 * 统一管理边缘节点的健康检查和权重计算逻辑
 */
@Service
public class NodeWeightCalculator {

    /**
     * CPU 使用率阈值 (%)
     */
    private static final double CPU_THRESHOLD = 80.0;

    /**
     * 内存使用率阈值 (%)
     */
    private static final double MEMORY_THRESHOLD = 85.0;

    /**
     * 权重因子：容量
     */
    private static final double CAPACITY_WEIGHT = 0.35;

    /**
     * 权重因子：CPU
     */
    private static final double CPU_WEIGHT = 0.25;

    /**
     * 权重因子：内存
     */
    private static final double MEMORY_WEIGHT = 0.25;

    /**
     * 权重因子：响应时间
     */
    private static final double RESPONSE_TIME_WEIGHT = 0.15;

    /**
     * 判断节点是否健康
     * 不健康条件：CPU >= 80% 或 内存 >= 85%
     * NULL 处理：NULL 视为满足条件（假设无数据表示正常）
     *
     * @param cpuUsage    CPU 使用率 (0-100, nullable)
     * @param memoryUsage 内存使用率 (0-100, nullable)
     * @return true 表示节点健康
     */
    public boolean isNodeHealthy(Double cpuUsage, Double memoryUsage) {
        // CPU 为 NULL 或低于阈值，且内存为 NULL 或低于阈值
        return (cpuUsage == null || cpuUsage < CPU_THRESHOLD)
            && (memoryUsage == null || memoryUsage < MEMORY_THRESHOLD);
    }

    /**
     * 计算节点权重（四因子加权）
     *
     * @param node        边缘节点
     * @param cpuUsage    CPU 使用率 (nullable)
     * @param memoryUsage 内存使用率 (nullable)
     * @return 权重值 (0-100)
     */
    public double calculateWeight(EdgeNode node, Double cpuUsage, Double memoryUsage) {
        // 不健康的节点权重为 0
        if (!isNodeHealthy(cpuUsage, memoryUsage)) {
            return 0.0;
        }

        // 四因子计算
        double capacityScore = calculateCapacityScore(node);
        double cpuScore = calculateCpuScore(cpuUsage);
        double memoryScore = calculateMemoryScore(memoryUsage);
        double responseTimeScore = calculateResponseTimeScore(node);

        // 加权平均并转换为 0-100
        return (capacityScore * CAPACITY_WEIGHT
              + cpuScore * CPU_WEIGHT
              + memoryScore * MEMORY_WEIGHT
              + responseTimeScore * RESPONSE_TIME_WEIGHT) * 100;
    }

    /**
     * 计算带区域加成的权重
     *
     * @param node          边缘节点
     * @param cpuUsage      CPU 使用率
     * @param memoryUsage   内存使用率
     * @param sourceRegionId 源节点区域 ID
     * @param bonusRate     加成比例 (例如 0.3 表示 30% 加成)
     * @return 加成后的权重值
     */
    public double calculateWeightWithRegionBonus(EdgeNode node, Double cpuUsage, Double memoryUsage,
                                                 Long sourceRegionId, double bonusRate) {
        double baseWeight = calculateWeight(node, cpuUsage, memoryUsage);

        // 同区域节点获得加成
        if (sourceRegionId != null && node.getRegionId() != null
            && sourceRegionId.equals(node.getRegionId())) {
            return baseWeight * (1 + bonusRate);
        }

        return baseWeight;
    }

    /**
     * 计算容量得分
     * 剩余容量越多，得分越高
     */
    private double calculateCapacityScore(EdgeNode node) {
        if (node.getMaxCameraSupport() == null || node.getMaxCameraSupport() == 0) {
            return 1.0; // 无限制，视为最优
        }
        double currentLoad = (double) node.getCurrentCameraCount() / node.getMaxCameraSupport();
        return Math.max(0, 1.0 - currentLoad);
    }

    /**
     * 计算 CPU 得分
     * CPU 使用率越低，得分越高
     */
    private double calculateCpuScore(Double cpuUsage) {
        if (cpuUsage == null) return 1.0;
        return Math.max(0, (100.0 - cpuUsage) / 100.0);
    }

    /**
     * 计算内存得分
     * 内存使用率越低，得分越高
     */
    private double calculateMemoryScore(Double memoryUsage) {
        if (memoryUsage == null) return 1.0;
        return Math.max(0, (100.0 - memoryUsage) / 100.0);
    }

    /**
     * 计算响应时间得分
     * 基于最后心跳时间，越近得分越高
     */
    private double calculateResponseTimeScore(EdgeNode node) {
        if (node.getLastHeartbeatTime() == null) {
            return 0.5; // 无心跳数据
        }

        long secondsSinceHeartbeat = Duration.between(
            node.getLastHeartbeatTime(), LocalDateTime.now()
        ).getSeconds();

        if (secondsSinceHeartbeat < 60) return 1.0;      // 1 分钟内
        if (secondsSinceHeartbeat < 300) return 0.7;     // 5 分钟内
        if (secondsSinceHeartbeat < 600) return 0.4;     // 10 分钟内
        return 0.1;                                      // 超过 10 分钟
    }
}
