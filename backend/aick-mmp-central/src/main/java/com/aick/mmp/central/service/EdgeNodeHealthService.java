package com.aick.mmp.central.service;

import com.aick.mmp.shared.model.CameraFailoverEvent;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class EdgeNodeHealthService {

    private final EdgeNodeRepository edgeNodeRepository;
    private final EdgeNodeFailoverService edgeNodeFailoverService; // 故障转移服务
    private static final int HEARTBEAT_TIMEOUT_MINUTES = 3; // 3分钟未收到心跳视为离线

    /**
     * 定时检查边缘节点健康状态
     * 每分钟执行一次
     */
    @Scheduled(fixedDelay = 60000) // 60秒 = 1分钟
    @Transactional
    public void checkEdgeNodeHealth() {
        log.debug("开始检查边缘节点健康状态...");
        
        // 获取所有在线或连接中的节点
        List<EdgeNode> activeNodes = edgeNodeRepository.findByStatusIn(List.of(
            EdgeNode.NodeStatus.ONLINE, 
            EdgeNode.NodeStatus.CONNECTING
        ));
        
        int offlineCount = 0;
        int totalChecked = 0;
        
        for (EdgeNode node : activeNodes) {
            totalChecked++;
            
            // 如果节点被禁用，跳过检查
            if (!node.isEnabled()) {
                continue;
            }
            
            // 检查是否有最近的心跳
            LocalDateTime lastHeartbeat = node.getLastHeartbeatTime();
            if (lastHeartbeat == null) {
                // 没有心跳记录，标记为离线
                markNodeOffline(node, "未收到过心跳");
                offlineCount++;
                continue;
            }
            
            // 计算距离上次心跳的时间间隔
            Duration duration = Duration.between(lastHeartbeat, LocalDateTime.now());
            long minutesSinceHeartbeat = duration.toMinutes();
            
            if (minutesSinceHeartbeat >= HEARTBEAT_TIMEOUT_MINUTES) {
                // 心跳超时，标记为离线
                markNodeOffline(node, String.format("心跳超时（%d分钟未收到心跳）", minutesSinceHeartbeat));
                offlineCount++;
            } else if (node.getStatus() != EdgeNode.NodeStatus.ONLINE) {
                // 如果有近期心跳但状态不是在线，更新为在线状态
                if (node.getStatus() == EdgeNode.NodeStatus.CONNECTING) {
                    node.setStatus(EdgeNode.NodeStatus.ONLINE);
                    node.setUpdatedAt(LocalDateTime.now());
                    edgeNodeRepository.save(node);
                    log.info("边缘节点 {} 连接成功，状态更新为在线", node.getName());
                }
            }
        }
        
        if (offlineCount > 0) {
            log.info("健康检查完成：检查了 {} 个节点，{} 个节点被标记为离线", totalChecked, offlineCount);
        } else if (totalChecked > 0) {
            log.debug("健康检查完成：检查了 {} 个节点，全部正常", totalChecked);
        }
    }
    
    /**
     * 标记节点为离线，并触发故障转移
     */
    private void markNodeOffline(EdgeNode node, String reason) {
        if (node.getStatus() != EdgeNode.NodeStatus.OFFLINE) {
            log.warn("标记边缘节点为离线: {} (ID: {}), 原因: {}", node.getName(), node.getId(), reason);
            node.setStatus(EdgeNode.NodeStatus.OFFLINE);
            node.setUpdatedAt(LocalDateTime.now());
            edgeNodeRepository.save(node);

            // 触发自动故障转移
            try {
                edgeNodeFailoverService.triggerFailover(node.getId(), CameraFailoverEvent.FailoverTriggerType.AUTO);
                log.info("已为离线节点 {} (ID: {}) 触发自动故障转移", node.getName(), node.getId());
            } catch (Exception e) {
                log.error("触发节点 {} 的故障转移失败: {}", node.getId(), e.getMessage(), e);
                // 故障转移失败不影响主流程
            }
        }
    }
    
    /**
     * 检查指定节点的健康状态
     */
    public String checkNodeHealthStatus(Long nodeId) {
        return edgeNodeRepository.findById(nodeId).map(node -> {
            if (!node.isEnabled()) {
                return "DISABLED";
            }
            
            LocalDateTime lastHeartbeat = node.getLastHeartbeatTime();
            if (lastHeartbeat == null) {
                return "NO_HEARTBEAT";
            }
            
            Duration duration = Duration.between(lastHeartbeat, LocalDateTime.now());
            if (duration.toMinutes() >= HEARTBEAT_TIMEOUT_MINUTES) {
                return "TIMEOUT";
            }
            
            // 检查系统指标
            if (node.getSystemMetrics() != null) {
                Map<String, Object> metrics = node.getSystemMetrics();
                
                // 检查CPU使用率
                if (metrics.containsKey("cpu_usage")) {
                    double cpuUsage = Double.parseDouble(metrics.get("cpu_usage").toString());
                    if (cpuUsage > 90.0) {
                        return "HIGH_CPU";
                    }
                }
                
                // 检查内存使用率
                if (metrics.containsKey("memory_usage")) {
                    double memoryUsage = Double.parseDouble(metrics.get("memory_usage").toString());
                    if (memoryUsage > 90.0) {
                        return "HIGH_MEMORY";
                    }
                }
                
                // 检查存储使用率
                if (metrics.containsKey("storage_usage")) {
                    double storageUsage = Double.parseDouble(metrics.get("storage_usage").toString());
                    if (storageUsage > 90.0) {
                        return "HIGH_STORAGE";
                    }
                }
            }
            
            // 检查摄像头负载
            if (node.getMaxCameraSupport() != null && node.getCurrentCameraCount() != null) {
                if (node.getCurrentCameraCount() >= node.getMaxCameraSupport()) {
                    return "HIGH_LOAD";
                }
            }
            
            return "HEALTHY";
        }).orElse("NOT_FOUND");
    }
    
    /**
     * 获取节点健康详情
     */
    public Map<String, Object> getNodeHealthDetails(Long nodeId) {
        Map<String, Object> details = new java.util.HashMap<>();
        
        return edgeNodeRepository.findById(nodeId).map(node -> {
            details.put("nodeId", node.getId());
            details.put("nodeName", node.getName());
            details.put("status", node.getStatus());
            details.put("enabled", node.isEnabled());
            details.put("lastHeartbeatTime", node.getLastHeartbeatTime());
            
            // 计算距离上次心跳的时间
            if (node.getLastHeartbeatTime() != null) {
                Duration duration = Duration.between(node.getLastHeartbeatTime(), LocalDateTime.now());
                details.put("secondsSinceLastHeartbeat", duration.getSeconds());
                details.put("minutesSinceLastHeartbeat", duration.toMinutes());
            }
            
            // 添加系统指标
            if (node.getSystemMetrics() != null) {
                details.put("systemMetrics", node.getSystemMetrics());
            }
            
            // 添加摄像头负载信息
            details.put("currentCameraCount", node.getCurrentCameraCount());
            details.put("maxCameraSupport", node.getMaxCameraSupport());
            if (node.getCurrentCameraCount() != null && node.getMaxCameraSupport() != null) {
                double loadPercentage = (node.getCurrentCameraCount() * 100.0) / node.getMaxCameraSupport();
                details.put("cameraLoadPercentage", loadPercentage);
            }
            
            // 计算总体健康评分 (0-100)
            double healthScore = 100.0;
            Map<String, Object> issues = new java.util.HashMap<>();
            
            // 检查心跳
            if (node.getLastHeartbeatTime() != null) {
                Duration duration = Duration.between(node.getLastHeartbeatTime(), LocalDateTime.now());
                if (duration.toMinutes() >= HEARTBEAT_TIMEOUT_MINUTES) {
                    healthScore -= 50; // 心跳超时严重问题
                    issues.put("heartbeat", "心跳超时");
                } else if (duration.toMinutes() >= 1) {
                    healthScore -= 10; // 心跳延迟小问题
                    issues.put("heartbeat", "心跳延迟");
                }
            } else {
                healthScore -= 50; // 无心跳记录
                issues.put("heartbeat", "无心跳记录");
            }
            
            // 检查系统指标
            if (node.getSystemMetrics() != null) {
                Map<String, Object> metrics = node.getSystemMetrics();
                
                // 检查CPU
                if (metrics.containsKey("cpu_usage")) {
                    double cpuUsage = Double.parseDouble(metrics.get("cpu_usage").toString());
                    if (cpuUsage > 90.0) {
                        healthScore -= 30;
                        issues.put("cpu", "CPU使用率过高");
                    } else if (cpuUsage > 80.0) {
                        healthScore -= 15;
                        issues.put("cpu", "CPU使用率偏高");
                    }
                }
                
                // 检查内存
                if (metrics.containsKey("memory_usage")) {
                    double memoryUsage = Double.parseDouble(metrics.get("memory_usage").toString());
                    if (memoryUsage > 90.0) {
                        healthScore -= 30;
                        issues.put("memory", "内存使用率过高");
                    } else if (memoryUsage > 80.0) {
                        healthScore -= 15;
                        issues.put("memory", "内存使用率偏高");
                    }
                }
            }
            
            // 确保健康评分不低于0
            healthScore = Math.max(0, healthScore);
            details.put("healthScore", Math.round(healthScore));
            details.put("healthIssues", issues);
            
            // 设置健康状态
            String healthStatus;
            if (healthScore >= 90) {
                healthStatus = "EXCELLENT";
            } else if (healthScore >= 70) {
                healthStatus = "GOOD";
            } else if (healthScore >= 50) {
                healthStatus = "FAIR";
            } else if (healthScore >= 30) {
                healthStatus = "POOR";
            } else {
                healthStatus = "CRITICAL";
            }
            details.put("healthStatus", healthStatus);
            
            return details;
        }).orElseThrow(() -> new RuntimeException("边缘节点不存在"));
    }
}