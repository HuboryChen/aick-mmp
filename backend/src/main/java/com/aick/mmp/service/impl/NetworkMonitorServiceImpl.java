package com.aick.mmp.service.impl;

import com.aick.mmp.model.EdgeNode;
import com.aick.mmp.model.CdnNode;
import com.aick.mmp.model.StreamSession;
import com.aick.mmp.repository.StreamSessionRepository;
import com.aick.mmp.service.CdnNodeService;
import com.aick.mmp.service.NetworkMonitorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkMonitorServiceImpl implements NetworkMonitorService {

    private final StreamSessionRepository streamSessionRepository;
    private final CdnNodeService cdnNodeService;


    // 网络指标阈值配置
    private static final double MAX_LATENCY_THRESHOLD = 300.0; // 毫秒
    private static final double MAX_PACKET_LOSS_THRESHOLD = 2.0; // 百分比
    private static final double MIN_BANDWIDTH_THRESHOLD = 5000.0; // kbps

    // 存储最近的网络指标用于趋势分析
    private final Map<String, NetworkMetricsHistory> metricsHistory = new ConcurrentHashMap<>();

    @Override
    public void evaluateAndAdjust(EdgeNode edgeNode, Map<String, Double> metrics) {
        if (metrics == null) return;

        String nodeId = edgeNode.getId().toString();
        NetworkMetricsHistory history = metricsHistory.computeIfAbsent(nodeId, k -> new NetworkMetricsHistory());
        history.addMetrics(metrics);

        // 检查是否需要调整
        boolean needAdjustment = checkMetricsThresholds(history.getRecentMetrics());

        if (needAdjustment) {
            log.warn("Network metrics threshold exceeded for edge node: {}", nodeId);
            adjustStreamsForEdgeNode(edgeNode, history.getRecentMetrics());
        }
    }

    @Override
    public void evaluateAndAdjustCdn(CdnNode cdnNode, Map<String, Double> metrics) {
        if (metrics == null) return;

        String nodeId = cdnNode.getId().toString();
        NetworkMetricsHistory history = metricsHistory.computeIfAbsent(nodeId, k -> new NetworkMetricsHistory());
        history.addMetrics(metrics);

        boolean needAdjustment = checkMetricsThresholds(history.getRecentMetrics());

        if (needAdjustment) {
            log.warn("Network metrics threshold exceeded for CDN node: {}", nodeId);
            cdnNodeService.updateCdnNodeStatus(cdnNode.getId(), "DEGRADED", "Network performance below threshold");
            redistributeStreamsFromCdn(cdnNode);
        } else if (cdnNode.getStatus() == CdnNode.NodeStatus.DEGRADED) {
            cdnNodeService.updateCdnNodeStatus(cdnNode.getId(), "ONLINE", "Network performance restored");
        }
    }

    /**
     * 检查网络指标是否超过阈值
     */
    private boolean checkMetricsThresholds(Map<String, Double> metrics) {
        double latency = metrics.getOrDefault("latency", 0.0);
        double packetLoss = metrics.getOrDefault("packetLoss", 0.0);
        double bandwidth = metrics.getOrDefault("bandwidth", 0.0);

        return latency > MAX_LATENCY_THRESHOLD ||
               packetLoss > MAX_PACKET_LOSS_THRESHOLD ||
               bandwidth < MIN_BANDWIDTH_THRESHOLD;
    }

    /**
     * 调整边缘节点上的流
     */
    private void adjustStreamsForEdgeNode(EdgeNode edgeNode, Map<String, Double> metrics) {
        String edgeNodeId = edgeNode.getId().toString();
        List<StreamSession> sessions = StringUtils.hasText(edgeNodeId) ? 
            streamSessionRepository.findByEdgeNodeId(edgeNodeId) : 
            Collections.emptyList();
        if (sessions.isEmpty()) return;

        double bandwidth = metrics.getOrDefault("bandwidth", 0.0);
        double packetLoss = metrics.getOrDefault("packetLoss", 0.0);

        // 根据网络状况调整流质量
        for (StreamSession session : sessions) {
            if (session == null || StringUtils.isEmpty(session.getSessionId())) {
                continue;
            }
            try {
                if (bandwidth < MIN_BANDWIDTH_THRESHOLD * 0.7) {
                    // 严重带宽不足，降低视频质量
                    log.warn("Stream quality adjustment not implemented: Low bandwidth for session {}", session.getSessionId());
                } else if (packetLoss > MAX_PACKET_LOSS_THRESHOLD) {
                    log.warn("Stream protocol switch not implemented: High packet loss for session {}", session.getSessionId());
                }
            } catch (Exception e) {
                log.error("Failed to adjust stream {}: {}", session.getSessionId(), e.getMessage());
            }
        }
    }

    /**
     * 重新分配来自性能下降CDN节点的流
     */
    private void redistributeStreamsFromCdn(CdnNode degradedNode) {
        // 在实际实现中，这里应该:
        // 1. 找到使用该CDN节点的所有流会话
        // 2. 为这些会话找到替代CDN节点
        // 3. 将流切换到新的CDN节点
        log.info("Redistributing streams from degraded CDN node: {}", degradedNode.getName());
    }

    /**
     * 定期检查所有节点的网络状态
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void scheduledNetworkCheck() {
        log.info("Performing scheduled network health check");
        // 实现定期检查逻辑
    }

    /**
     * 网络指标历史记录类
     */
    private static class NetworkMetricsHistory {
        private final Map<String, Double> recentMetrics = new ConcurrentHashMap<>();
        private LocalDateTime lastUpdated;

        public void addMetrics(Map<String, Double> metrics) {
            recentMetrics.putAll(metrics);
            lastUpdated = LocalDateTime.now();
        }

        public Map<String, Double> getRecentMetrics() {
            return new ConcurrentHashMap<>(recentMetrics);
        }

        public LocalDateTime getLastUpdated() {
            return lastUpdated;
        }
    }
}