package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.config.EdgeFailoverProperties;
import com.aick.mmp.central.repository.CameraFailoverEventRepository;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.CameraFailoverEvent;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.central.service.EdgeNodeFailoverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 边缘节点故障转移服务实现
 *
 * 核心流程：
 * 1. 查询源节点上的在线摄像头（候选列表）
 * 2. 过滤已迁移的摄像头（避免重复）
 * 3. 创建故障转移事件记录
 * 4. 分批处理：每批选择最优目标节点 → 批量更新 → 失败的进入待分配池
 * 5. 更新事件最终状态
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EdgeNodeFailoverServiceImpl implements EdgeNodeFailoverService {

    private final CameraRepository cameraRepository;
    private final EdgeNodeRepository edgeNodeRepository;
    private final CameraFailoverEventRepository failoverEventRepository;
    private final EdgeFailoverProperties failoverProperties;
    private final com.aick.mmp.central.service.NodeWeightCalculator nodeWeightCalculator;

    /**
     * 并发控制信号量，限制同时进行的故障转移任务数
     */
    private final Semaphore failoverSemaphore = new Semaphore(3);

    @Override
    public Long triggerFailover(Long sourceNodeId, CameraFailoverEvent.FailoverTriggerType triggerType) {
        log.info("[故障转移] 触发节点 {} 的故障转移, 类型: {}", sourceNodeId, triggerType);

        if (!failoverProperties.isEnabled()) {
            log.info("[故障转移] 功能已禁用, 跳过执行");
            return null;
        }

        // 验证源节点存在
        EdgeNode sourceNode = edgeNodeRepository.findById(sourceNodeId)
                .orElseThrow(() -> new IllegalArgumentException("边缘节点不存在: " + sourceNodeId));

        // 异步模式：提交到异步线程池
        if (failoverProperties.getMode() == EdgeFailoverProperties.FailoverMode.ASYNC) {
            executeFailoverAsync(sourceNodeId, triggerType, sourceNode);
            return null; // 异步模式下不立即返回事件ID
        }

        // 同步模式：直接执行
        return doExecuteFailover(sourceNodeId, triggerType, sourceNode);
    }

    /**
     * 异步执行故障转移
     */
    @Async("taskExecutor")
    public void executeFailoverAsync(Long sourceNodeId, CameraFailoverEvent.FailoverTriggerType triggerType,
                                       EdgeNode sourceNode) {
        try {
            // 尝试获取信号量许可
            if (!failoverSemaphore.tryAcquire(failoverProperties.getMaxConcurrentTasks(), 10, TimeUnit.SECONDS)) {
                log.warn("[故障转移] 并发任务数已达上限 ({})，跳过节点 {} 的故障转移",
                        failoverProperties.getMaxConcurrentTasks(), sourceNodeId);
                return;
            }
            try {
                // 延迟等待机制：检查延迟期间节点是否恢复
                if (failoverProperties.getDelaySeconds() > 0) {
                    boolean recovered = waitForRecovery(sourceNodeId, failoverProperties.getDelaySeconds());
                    if (recovered) {
                        log.info("[故障转移] 节点 {} 在延迟期内已恢复，取消故障转移", sourceNodeId);
                        return;
                    }
                }
                doExecuteFailover(sourceNodeId, triggerType, sourceNode);
            } finally {
                failoverSemaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[故障转移] 节点 {} 的故障转移被中断", sourceNodeId);
        } catch (Exception e) {
            log.error("[故障转移] 节点 {} 的故障转移异常: {}", sourceNodeId, e.getMessage(), e);
        }
    }

    /**
     * 同步执行故障转移的核心逻辑
     */
    @Transactional
    public Long doExecuteFailover(Long sourceNodeId, CameraFailoverEvent.FailoverTriggerType triggerType,
                                    EdgeNode sourceNode) {

        // 1. 查询该节点上的所有在线摄像头
        List<Camera> candidateCameras = cameraRepository.findByEdgeNodeIdAndStatus(
                sourceNodeId, Camera.CameraStatus.ONLINE);

        if (candidateCameras.isEmpty()) {
            log.info("[故障转移] 节点 {} 上没有需要迁移的在线摄像头", sourceNodeId);
            return null;
        }

        log.info("[故障转移] 发现 {} 个候选摄像头需要从节点 {} 迁移",
                candidateCameras.size(), sourceNodeId);

        // 2. 过滤已被其他流程迁移的摄像头（重复检测）
        List<Camera> camerasToMigrate = candidateCameras.stream()
                .filter(c -> Objects.equals(c.getEdgeNodeId(), sourceNodeId))
                .collect(Collectors.toList());

        long skippedCount = candidateCameras.size() - camerasToMigrate.size();
        if (skippedCount > 0) {
            log.info("[故障转移] 跳过 {} 个已被迁移的摄像头", skippedCount);
        }

        if (camerasToMigrate.isEmpty()) {
            log.info("[故障转移] 所有候选摄像头已被其他流程迁移");
            return null;
        }

        // 3. 创建故障转移事件记录
        CameraFailoverEvent event = CameraFailoverEvent.builder()
                .sourceEdgeNodeId(sourceNodeId)
                .totalCount(camerasToMigrate.size())
                .successCount(0)
                .failedCount(0)
                .triggerType(triggerType)
                .status(CameraFailoverEvent.FailoverStatus.IN_PROGRESS)
                .build();

        event = failoverEventRepository.save(event);
        Long eventId = event.getId();
        log.info("[故障转移] 创建事件 ID={}, 候选数={}", eventId, camerasToMigrate.size());

        // 4. 分批处理
        Set<Long> allTargetNodeIds = new HashSet<>();
        int successCount = 0;
        int failedCount = 0;

        int batchSize = failoverProperties.getBatchSize();
        List<List<Camera>> batches = partitionList(camerasToMigrate, batchSize);

        for (int i = 0; i < batches.size(); i++) {
            List<Camera> batch = batches.get(i);
            log.info("[故障转移] 处理批次 {}/{}, 数量={}", i + 1, batches.size(), batch.size());

            BatchResult result = processBatch(batch, sourceNode, eventId);
            successCount += result.successCount;
            failedCount += result.failedCount;
            allTargetNodeIds.addAll(result.targetNodeIds);

            // 批次间等待
            if (i < batches.size() - 1 && failoverProperties.getBatchDelayMs() > 0) {
                try {
                    Thread.sleep(failoverProperties.getBatchDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[故障转移] 批次间等待被中断");
                    break;
                }
            }
        }

        // 5. 更新事件最终状态
        event.setSuccessCount(successCount);
        event.setFailedCount(failedCount);
        event.setTargetEdgeNodeIds(new ArrayList<>(allTargetNodeIds));
        event.setCameraIds(camerasToMigrate.stream().map(Camera::getId).collect(Collectors.toList()));
        event.setCompletedAt(LocalDateTime.now());

        if (failedCount == 0) {
            event.setStatus(CameraFailoverEvent.FailoverStatus.COMPLETED);
        } else if (successCount == 0) {
            event.setStatus(CameraFailoverEvent.FailoverStatus.FAILED);
        } else {
            event.setStatus(CameraFailoverEvent.FailoverStatus.PARTIAL);
        }

        failoverEventRepository.save(event);

        log.info("[故障转移] 完成! 事件ID={}, 成功={}, 失败={}, 状态={}",
                eventId, successCount, failedCount, event.getStatus());

        return eventId;
    }

    /**
     * 处理单个批次
     * 在事务内批量更新摄像头的 edgeNodeId
     */
    @Transactional
    public BatchResult processBatch(List<Camera> batch, EdgeNode sourceNode, Long eventId) {
        BatchResult result = new BatchResult();
        List<Camera> successfulMigrations = new ArrayList<>();

        for (Camera camera : batch) {
            // 再次检查是否已被迁移（防止并发情况）
            if (!Objects.equals(camera.getEdgeNodeId(), sourceNode.getId())) {
                log.debug("[故障转移] 摄像头 {} 已被跳过（edgeNodeId 已变更）", camera.getId());
                continue;
            }

            try {
                // 选择目标节点
                Long targetNodeId = selectTargetNodeForFailover(camera, sourceNode);
                if (targetNodeId != null) {
                    camera.setEdgeNodeId(targetNodeId);
                    camera.setStatus(Camera.CameraStatus.CONNECTING); // 迁移中设为连接中
                    camera.setUpdatedAt(LocalDateTime.now());
                    successfulMigrations.add(camera);
                    result.targetNodeIds.add(targetNodeId);
                } else {
                    // 无可用节点，进入待分配池
                    log.warn("[故障转移] 摄像头 {} 无法找到可用目标节点，进入待分配池", camera.getId());
                    camera.setEdgeNodeId(null);
                    camera.setStatus(Camera.CameraStatus.PENDING_ALLOCATION);
                    camera.setUpdatedAt(LocalDateTime.now());
                    result.failedCount++;
                }
            } catch (Exception e) {
                log.error("[故障转移] 摄像头 {} 迁移失败: {}", camera.getId(), e.getMessage());
                // 进入待分配池
                camera.setEdgeNodeId(null);
                camera.setStatus(Camera.CameraStatus.PENDING_ALLOCATION);
                camera.setUpdatedAt(LocalDateTime.now());
                result.failedCount++;
            }
        }

        // 保存整个批次
        if (!successfulMigrations.isEmpty()) {
            cameraRepository.saveAll(successfulMigrations);
            result.successCount = successfulMigrations.size();
        }

        return result;
    }

    /**
     * 为故障转移选择最优目标节点
     * 复用现有四因子权重算法 + 地域bonus加成 + 容量校验
     *
     * @param camera    待迁移的摄像头
     * @param sourceNode 源节点（用于获取区域信息做亲和性匹配）
     * @return 目标节点ID，无可用时返回null
     */
    public Long selectTargetNodeForFailover(Camera camera, EdgeNode sourceNode) {
        // 获取所有在线节点（排除源节点）
        List<EdgeNode> onlineNodes = edgeNodeRepository.findByStatus(EdgeNode.NodeStatus.ONLINE).stream()
                .filter(n -> !n.getId().equals(sourceNode.getId()))
                .collect(Collectors.toList());

        if (onlineNodes.isEmpty()) {
            return null;
        }

        // 计算每个节点的权重
        Map<EdgeNode, Double> nodeWeights = new HashMap<>();
        for (EdgeNode node : onlineNodes) {
            // 容量检查
            if (node.getMaxCameraSupport() != null && node.getCurrentCameraCount() != null
                    && node.getCurrentCameraCount() >= node.getMaxCameraSupport()) {
                continue; // 跳过已满载的节点
            }

            double weight = calculateFailoverWeight(node, sourceNode);
            nodeWeights.put(node, weight);
        }

        if (nodeWeights.isEmpty()) {
            return null;
        }

        // 选择权重最高的节点
        return nodeWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .map(EdgeNode::getId)
                .orElse(null);
    }

    /**
     * 计算故障转移模式下的节点权重
     * 基于 NodeWeightCalculator × (1 + regionBonus)
     */
    private double calculateFailoverWeight(EdgeNode targetNode, EdgeNode sourceNode) {
        // 使用 NodeWeightCalculator 计算基础权重（包含健康检查）
        Double cpuUsage = targetNode.getCpuUsage();
        Double memoryUsage = targetNode.getMemoryUsage();

        // 如果节点不健康，返回 0
        if (!nodeWeightCalculator.isNodeHealthy(cpuUsage, memoryUsage)) {
            return 0.0;
        }

        // 使用共享服务计算权重（带区域加成）
        double weight = nodeWeightCalculator.calculateWeightWithRegionBonus(
                targetNode,
                cpuUsage,
                memoryUsage,
                sourceNode.getRegionId(),
                failoverProperties.getRegionBonus()
        );

        return weight;
    }

    /**
     * 基础节点权重计算（使用 NodeWeightCalculator）
     */
    private double calculateBaseNodeWeight(EdgeNode node) {
        return nodeWeightCalculator.calculateWeight(
                node,
                node.getCpuUsage(),
                node.getMemoryUsage()
        );
    }

    @Override
    @Transactional
    public int processPendingAllocationPool() {
        log.info("[待分配池] 开始处理待分配的摄像头");

        // 查询待分配池中的摄像头
        Page<Camera> pendingPage = cameraRepository.findByStatus(
                Camera.CameraStatus.PENDING_ALLOCATION,
                org.springframework.data.domain.PageRequest.of(0, 1000));
        List<Camera> pendingCameras = pendingPage.getContent().stream()
                .filter(c -> c.getEdgeNodeId() == null)
                .collect(Collectors.toList());

        if (pendingCameras.isEmpty()) {
            log.debug("[待分配池] 没有待分配的摄像头");
            return 0;
        }

        log.info("[待分配池] 发现 {} 个待分配摄像头", pendingCameras.size());

        int allocatedCount = 0;
        for (Camera camera : pendingCameras) {
            try {
                // 使用标准的最优节点选择（不使用地域偏好）
                Long optimalNodeId = findAnyAvailableNode();
                if (optimalNodeId != null) {
                    camera.setEdgeNodeId(optimalNodeId);
                    camera.setStatus(Camera.CameraStatus.CONNECTING);
                    camera.setUpdatedAt(LocalDateTime.now());
                    cameraRepository.save(camera);
                    allocatedCount++;
                    log.debug("[待分配池] 摄像头 {} 已分配到节点 {}", camera.getId(), optimalNodeId);
                }
            } catch (Exception e) {
                log.error("[待分配池] 摄像头 {} 分配失败: {}", camera.getId(), e.getMessage());
            }
        }

        if (allocatedCount > 0) {
            log.info("[待分配池] 成功分配 {}/{} 个摄像头", allocatedCount, pendingCameras.size());
        }

        return allocatedCount;
    }

    /**
     * 查找任意可用的在线健康节点（用于待分配池分配）
     */
    private Long findAnyAvailableNode() {
        List<EdgeNode> onlineNodes = edgeNodeRepository.findByStatus(EdgeNode.NodeStatus.ONLINE);
        for (EdgeNode node : onlineNodes) {
            if (node.getMaxCameraSupport() != null && node.getCurrentCameraCount() != null
                    && node.getCurrentCameraCount() < node.getMaxCameraSupport()) {
                return node.getId();
            }
        }
        // 如果所有节点都满载，返回第一个在线节点（允许超限分配）
        if (!onlineNodes.isEmpty()) {
            return onlineNodes.get(0).getId();
        }
        return null;
    }

    /**
     * 等待指定时间内节点是否恢复
     *
     * @return true 如果节点恢复为ONLINE
     */
    private boolean waitForRecovery(Long nodeId, int delaySeconds) {
        log.info("[故障转移] 等待 {} 秒以确认节点 {} 是否恢复...", delaySeconds, nodeId);
        try {
            for (int i = 0; i < delaySeconds; i++) {
                Thread.sleep(1000);
                // 每秒检查一次节点状态
                EdgeNode node = edgeNodeRepository.findById(nodeId).orElse(null);
                if (node != null && node.getStatus() == EdgeNode.NodeStatus.ONLINE) {
                    log.info("[故障转移] 节点 {} 在第 {} 秒恢复!", nodeId, i + 1);
                    return true;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /**
     * 将列表分割为指定大小的批次
     */
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }

    /**
     * 批次处理结果
     */
    private static class BatchResult {
        int successCount = 0;
        int failedCount = 0;
        Set<Long> targetNodeIds = new HashSet<>();
    }
}
