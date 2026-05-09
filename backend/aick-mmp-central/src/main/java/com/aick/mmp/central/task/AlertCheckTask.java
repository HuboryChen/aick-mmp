package com.aick.mmp.central.task;

import com.aick.mmp.central.repository.AlertRuleRepository;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.service.AlertRecordService;
import com.aick.mmp.shared.model.AlertRule;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.EdgeNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 告警规则定时检查任务
 * 定期检查系统指标并触发告警
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertCheckTask {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRecordService alertRecordService;
    private final CameraRepository cameraRepository;
    private final EdgeNodeRepository edgeNodeRepository;

    /**
     * 每分钟检查一次告警规则
     */
    @Scheduled(fixedRate = 60000)
    public void checkAlertRules() {
        log.debug("Starting alert rule check");
        
        try {
            List<AlertRule> enabledRules = alertRuleRepository.findByEnabledTrue();
            
            for (AlertRule rule : enabledRules) {
                checkRule(rule);
            }
            
            log.debug("Completed alert rule check, processed {} rules", enabledRules.size());
        } catch (Exception e) {
            log.error("Error during alert rule check", e);
        }
    }

    private void checkRule(AlertRule rule) {
        try {
            switch (rule.getAlertType()) {
                case CAMERA_OFFLINE -> checkCameraOffline(rule);
                case EDGE_NODE_OFFLINE -> checkEdgeNodeOffline(rule);
                case CPU_USAGE -> checkCpuUsage(rule);
                case MEMORY_USAGE -> checkMemoryUsage(rule);
                case DISK_USAGE -> checkDiskUsage(rule);
                default -> log.debug("Skipping unhandled alert type: {}", rule.getAlertType());
            }
        } catch (Exception e) {
            log.error("Error checking rule {}: {}", rule.getId(), e.getMessage());
        }
    }

    /**
     * 检查摄像头离线告警
     */
    private void checkCameraOffline(AlertRule rule) {
        List<Camera> offlineCameras = cameraRepository.findByStatus(Camera.CameraStatus.OFFLINE);
        
        for (Camera camera : offlineCameras) {
            // 检查目标匹配
            if (rule.getTargetType() == AlertRule.TargetType.CAMERA && 
                    !rule.getTargetId().equals(camera.getId())) {
                continue;
            }
            
            String title = "摄像头离线告警";
            String message = String.format("摄像头 '%s' 已离线，位置: %s", 
                    camera.getName(), camera.getLocation());
            
            alertRecordService.createAlert(
                    rule,
                    AlertRule.AlertLevel.ERROR,
                    title,
                    message,
                    camera.getId(),
                    camera.getName(),
                    null,
                    null
            );
        }
    }

    /**
     * 检查边缘节点离线告警
     */
    private void checkEdgeNodeOffline(AlertRule rule) {
        List<EdgeNode> offlineNodes = edgeNodeRepository.findByStatus(EdgeNode.NodeStatus.OFFLINE);
        
        for (EdgeNode node : offlineNodes) {
            // 检查目标匹配
            if (rule.getTargetType() == AlertRule.TargetType.EDGE_NODE && 
                    !rule.getTargetId().equals(node.getId())) {
                continue;
            }
            
            String title = "边缘节点离线告警";
            String message = String.format("边缘节点 '%s' (UUID: %s) 已离线", 
                    node.getName(), node.getUuid());
            
            alertRecordService.createAlert(
                    rule,
                    AlertRule.AlertLevel.ERROR,
                    title,
                    message,
                    node.getId(),
                    node.getName(),
                    null,
                    null
            );
        }
    }

    /**
     * 检查CPU使用率告警
     */
    private void checkCpuUsage(AlertRule rule) {
        // 获取边缘节点的最新指标
        // 这里需要根据实际的心跳数据来实现
        // 暂时模拟检查逻辑
    }

    /**
     * 检查内存使用率告警
     */
    private void checkMemoryUsage(AlertRule rule) {
        // 获取边缘节点的最新指标
    }

    /**
     * 检查磁盘使用率告警
     */
    private void checkDiskUsage(AlertRule rule) {
        // 获取边缘节点的最新指标
    }
}
