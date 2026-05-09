package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.channel.NotificationChannel;
import com.aick.mmp.central.channel.NotificationChannelFactory;
import com.aick.mmp.central.channel.NotificationResult;
import com.aick.mmp.central.channel.NotificationSenderService;
import com.aick.mmp.central.repository.AlertEscalationRepository;
import com.aick.mmp.central.repository.AlertNotificationRepository;
import com.aick.mmp.central.repository.AlertRecordRepository;
import com.aick.mmp.central.repository.AlertRuleRepository;
import com.aick.mmp.central.service.EscalationService;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.AlertEscalation;
import com.aick.mmp.shared.model.AlertEscalation.*;
import com.aick.mmp.shared.model.AlertNotification;
import com.aick.mmp.shared.model.AlertRecord;
import com.aick.mmp.shared.model.AlertRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 告警升级服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EscalationServiceImpl implements EscalationService {

    private final AlertEscalationRepository escalationRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertNotificationRepository notificationRepository;
    private final NotificationSenderService notificationSenderService;
    private final NotificationChannelFactory channelFactory;
    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${escalation.check.enabled:true}")
    private boolean escalationCheckEnabled;

    @Value("${escalation.check.interval:60000}")
    private long checkInterval;

    @Override
    public void checkAndEscalate() {
        if (!escalationCheckEnabled) {
            log.debug("Escalation check is disabled");
            return;
        }

        log.debug("Starting escalation check...");

        // 获取所有待处理的升级配置
        List<AlertEscalation> pendingEscalations = escalationRepository.findPendingEscalations();
        
        int escalatedCount = 0;
        for (AlertEscalation escalation : pendingEscalations) {
            try {
                // 获取关联的告警记录
                Optional<AlertRecord> alertOpt = alertRecordRepository.findById(escalation.getAlertRecordId());
                if (alertOpt.isEmpty()) {
                    log.warn("Alert record {} not found for escalation {}", 
                             escalation.getAlertRecordId(), escalation.getId());
                    escalation.setStatus(EscalationStatus.SKIPPED);
                    escalation.setResult("Alert record not found");
                    escalationRepository.save(escalation);
                    continue;
                }

                AlertRecord alertRecord = alertOpt.get();

                // 检查告警是否已解决（解决后不再升级）
                if (alertRecord.getStatus() == AlertRecord.AlertStatus.RESOLVED ||
                    alertRecord.getStatus() == AlertRecord.AlertStatus.IGNORED) {
                    escalation.setStatus(EscalationStatus.SKIPPED);
                    escalation.setResult("Alert already resolved or ignored");
                    escalationRepository.save(escalation);
                    continue;
                }

                // 检查升级条件
                if (checkCondition(escalation, alertRecord)) {
                    executeEscalationAction(escalation, alertRecord);
                    escalatedCount++;
                }
            } catch (Exception e) {
                log.error("Error processing escalation {}: {}", escalation.getId(), e.getMessage(), e);
                escalation.setStatus(EscalationStatus.FAILED);
                escalation.setResult("Error: " + e.getMessage());
                escalationRepository.save(escalation);
            }
        }

        if (escalatedCount > 0) {
            log.info("Escalated {} alerts", escalatedCount);
        }
    }

    @Override
    public List<AlertEscalation> createEscalationsForAlert(AlertRecord alertRecord) {
        // 获取告警规则
        Optional<AlertRule> ruleOpt = alertRuleRepository.findById(alertRecord.getRuleId());
        if (ruleOpt.isEmpty()) {
            log.warn("Rule {} not found for alert {}", alertRecord.getRuleId(), alertRecord.getId());
            return Collections.emptyList();
        }

        AlertRule rule = ruleOpt.get();

        // 获取规则关联的升级配置模板
        List<AlertNotification> notifications = notificationRepository.findByRuleId(rule.getId());
        
        List<AlertEscalation> escalations = new ArrayList<>();
        
        for (AlertNotification notification : notifications) {
            // 如果通知配置包含升级信息，则创建升级配置
            if (notification.getEscalationEnabled() != null && notification.getEscalationEnabled()) {
                AlertEscalation escalation = buildEscalationFromNotification(alertRecord, notification);
                escalations.add(escalation);
            }
        }

        if (!escalations.isEmpty()) {
            List<AlertEscalation> saved = escalationRepository.saveAll(escalations);
            log.info("Created {} escalation configs for alert {}", saved.size(), alertRecord.getId());
            return saved;
        }

        return Collections.emptyList();
    }

    /**
     * 从通知配置构建升级配置
     */
    private AlertEscalation buildEscalationFromNotification(AlertRecord alertRecord, 
                                                            AlertNotification notification) {
        return AlertEscalation.builder()
                .ruleId(alertRecord.getRuleId())
                .alertRecordId(alertRecord.getId())
                .escalationLevel(1)
                .conditionType(EscalationConditionType.TIME_UNRESOLVED)
                .conditionValue(notification.getEscalationDelayMinutes() != null ? 
                                notification.getEscalationDelayMinutes() : 30)
                .actionType(EscalationAction.NOTIFY)
                .channelType(notification.getChannelType())
                .target(notification.getTarget())
                .titleTemplate("[升级告警] " + alertRecord.getTitle())
                .contentTemplate(buildEscalationContent(alertRecord))
                .notificationConfig(notification.getExtraConfig())
                .isTriggered(false)
                .status(EscalationStatus.PENDING)
                .build();
    }

    /**
     * 构建升级内容
     */
    private String buildEscalationContent(AlertRecord alertRecord) {
        return String.format("告警升级通知\\n\\n" +
                "告警标题: %s\\n" +
                "告警级别: %s\\n" +
                "发生时间: %s\\n" +
                "当前状态: %s\\n" +
                "持续时间: 未解决超过指定时长\\n\\n" +
                "请及时处理！",
                alertRecord.getTitle(),
                alertRecord.getLevel(),
                alertRecord.getAlertTime(),
                alertRecord.getStatus());
    }

    @Override
    public void triggerEscalation(Long escalationId) {
        AlertEscalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new ServiceException("Escalation not found: " + escalationId));

        AlertRecord alertRecord = alertRecordRepository.findById(escalation.getAlertRecordId())
                .orElseThrow(() -> new ServiceException("Alert record not found: " + escalation.getAlertRecordId()));

        if (escalation.getIsTriggered()) {
            log.warn("Escalation {} already triggered", escalationId);
            return;
        }

        executeEscalationAction(escalation, alertRecord);
    }

    @Override
    public void triggerAllEscalations(Long alertRecordId) {
        List<AlertEscalation> escalations = escalationRepository.findByAlertRecordId(alertRecordId);
        
        AlertRecord alertRecord = alertRecordRepository.findById(alertRecordId)
                .orElseThrow(() -> new ServiceException("Alert record not found: " + alertRecordId));

        for (AlertEscalation escalation : escalations) {
            if (!escalation.getIsTriggered()) {
                try {
                    executeEscalationAction(escalation, alertRecord);
                } catch (Exception e) {
                    log.error("Error triggering escalation {}: {}", escalation.getId(), e.getMessage());
                }
            }
        }
    }

    @Override
    public void skipEscalation(Long escalationId) {
        AlertEscalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new ServiceException("Escalation not found: " + escalationId));

        escalation.setStatus(EscalationStatus.SKIPPED);
        escalation.setResult("Manually skipped");
        escalationRepository.save(escalation);

        log.info("Escalation {} skipped", escalationId);
    }

    @Override
    public List<AlertEscalation> getEscalationHistory(Long alertRecordId) {
        return escalationRepository.findByAlertRecordIdAndIsTriggeredTrue(alertRecordId);
    }

    @Override
    public boolean checkCondition(AlertEscalation escalation, AlertRecord alertRecord) {
        EscalationConditionType conditionType = escalation.getConditionType();
        Integer conditionValue = escalation.getConditionValue();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkTime;

        switch (conditionType) {
            case TIME_UNACKED:
                // 未确认时长
                if (alertRecord.getAcknowledgedAt() != null) {
                    return false; // 已确认，不再检查
                }
                checkTime = alertRecord.getAlertTime();
                break;
                
            case TIME_UNRESOLVED:
                // 未解决时长
                if (alertRecord.getStatus() == AlertRecord.AlertStatus.RESOLVED ||
                    alertRecord.getStatus() == AlertRecord.AlertStatus.IGNORED) {
                    return false; // 已解决
                }
                checkTime = alertRecord.getAlertTime();
                break;
                
            case REPEAT_COUNT:
                // 重复次数（通过查询同一规则的未解决告警数量）
                long unresolvedCount = alertRecordRepository.countByCameraIdAndStatus(
                        alertRecord.getCameraId(), AlertRecord.AlertStatus.UNRESOLVED);
                return unresolvedCount >= conditionValue;
                
            case SEVERITY:
                // 严重程度升级（通过比较告警级别）
                return shouldEscalateForSeverity(alertRecord);
                
            default:
                return false;
        }

        // 计算时长
        long minutesElapsed = Duration.between(checkTime, now).toMinutes();
        boolean shouldEscalate = minutesElapsed >= conditionValue;

        if (shouldEscalate) {
            log.debug("Escalation condition met for alert {}: {} {} minutes (elapsed: {})",
                      alertRecord.getId(), conditionType, conditionValue, minutesElapsed);
        }

        return shouldEscalate;
    }

    /**
     * 检查是否应该因严重程度升级
     */
    private boolean shouldEscalateForSeverity(AlertRecord alertRecord) {
        // 根据告警级别判断是否需要升级
        // 例如：INFO -> WARNING -> CRITICAL 逐级升级
        return alertRecord.getLevel() == AlertRule.AlertLevel.CRITICAL;
    }

    @Override
    public void executeEscalationAction(AlertEscalation escalation, AlertRecord alertRecord) {
        log.info("Executing escalation {} for alert {}: {} action",
                 escalation.getId(), alertRecord.getId(), escalation.getActionType());

        escalation.setStatus(EscalationStatus.EXECUTING);
        escalationRepository.save(escalation);

        try {
            switch (escalation.getActionType()) {
                case NOTIFY:
                    executeNotifyAction(escalation, alertRecord);
                    break;
                case ASSIGN:
                    executeAssignAction(escalation, alertRecord);
                    break;
                case ESCALATE:
                    executeEscalateAction(escalation, alertRecord);
                    break;
                case AUTO_RESOLVE:
                    executeAutoResolveAction(alertRecord);
                    break;
                case CREATE_TICKET:
                    executeCreateTicketAction(escalation, alertRecord);
                    break;
                case EXECUTE_WEBHOOK:
                    executeWebhookAction(escalation, alertRecord);
                    break;
                default:
                    throw new ServiceException("Unknown action type: " + escalation.getActionType());
            }

            // 标记为已触发
            escalation.setIsTriggered(true);
            escalation.setTriggeredAt(LocalDateTime.now());
            escalation.setStatus(EscalationStatus.COMPLETED);
            escalation.setResult("Escalation executed successfully");
            escalationRepository.save(escalation);

        } catch (Exception e) {
            log.error("Failed to execute escalation {}: {}", escalation.getId(), e.getMessage());
            escalation.setStatus(EscalationStatus.FAILED);
            escalation.setResult("Execution failed: " + e.getMessage());
            escalationRepository.save(escalation);
            throw e;
        }
    }

    /**
     * 执行通知动作
     */
    private void executeNotifyAction(AlertEscalation escalation, AlertRecord alertRecord) {
        String title = formatTemplate(escalation.getTitleTemplate(), alertRecord);
        String content = formatTemplate(escalation.getContentTemplate(), alertRecord);
        
        // 构建通知配置
        AlertNotification notification = AlertNotification.builder()
                .channelType(escalation.getChannelType())
                .target(escalation.getTarget())
                .titleTemplate(title)
                .contentTemplate(content)
                .extraConfig(escalation.getNotificationConfig())
                .isEnabled(true)
                .build();

        var channelOpt = channelFactory.getChannel(escalation.getChannelType().name());
        if (channelOpt.isPresent()) {
            NotificationChannel channel = channelOpt.get();
            if (channel.isAvailable()) {
                // 更新告警记录的标题和消息
                String originalTitle = alertRecord.getTitle();
                String originalMessage = alertRecord.getMessage();
                alertRecord.setTitle(title);
                alertRecord.setMessage(content);
                
                NotificationResult result = channel.send(alertRecord, escalation.getTarget(), 
                        escalation.getNotificationConfig());
                
                // 恢复原始值
                alertRecord.setTitle(originalTitle);
                alertRecord.setMessage(originalMessage);
                
                if (!result.isSuccess()) {
                    throw new ServiceException("Notification failed: " + result.getErrorMessage());
                }
            } else {
                throw new ServiceException("Channel not available: " + escalation.getChannelType());
            }
        } else {
            log.warn("Channel {} not available, using fallback", escalation.getChannelType());
            // 使用通知发送服务作为备选
            NotificationResult result = notificationSenderService.sendToChannel(
                    alertRecord,
                    escalation.getChannelType().name(),
                    escalation.getTarget(),
                    escalation.getNotificationConfig()
            );
            
            if (!result.isSuccess()) {
                throw new ServiceException("Notification failed: " + result.getErrorMessage());
            }
        }
    }

    /**
     * 执行分配动作
     */
    private void executeAssignAction(AlertEscalation escalation, AlertRecord alertRecord) {
        // 从配置中获取分配目标
        String assignee = escalation.getNotificationConfig();
        
        alertRecord.setResolvedBy(null); // 清空当前处理人
        alertRecord.setStatus(AlertRecord.AlertStatus.UNRESOLVED);
        alertRecordRepository.save(alertRecord);

        log.info("Alert {} assigned to {}", alertRecord.getId(), assignee);
    }

    /**
     * 执行继续升级动作
     */
    private void executeEscalateAction(AlertEscalation escalation, AlertRecord alertRecord) {
        // 创建下一级升级
        int nextLevel = escalation.getEscalationLevel() + 1;
        
        AlertEscalation nextEscalation = AlertEscalation.builder()
                .ruleId(escalation.getRuleId())
                .alertRecordId(alertRecord.getId())
                .escalationLevel(nextLevel)
                .conditionType(escalation.getConditionType())
                .conditionValue(escalation.getConditionValue() * 2) // 升级条件翻倍
                .actionType(escalation.getActionType())
                .channelType(escalation.getChannelType())
                .target(escalation.getTarget())
                .titleTemplate(escalation.getTitleTemplate())
                .contentTemplate(escalation.getContentTemplate())
                .notificationConfig(escalation.getNotificationConfig())
                .isTriggered(false)
                .status(EscalationStatus.PENDING)
                .build();

        escalationRepository.save(nextEscalation);
        log.info("Created next level escalation {} for alert {}", nextLevel, alertRecord.getId());
    }

    /**
     * 执行自动解决动作
     */
    private void executeAutoResolveAction(AlertRecord alertRecord) {
        alertRecord.setStatus(AlertRecord.AlertStatus.AUTO_RESOLVED);
        alertRecord.setResolvedAt(LocalDateTime.now());
        alertRecord.setResolutionNote("Auto resolved by escalation");
        alertRecordRepository.save(alertRecord);

        log.info("Alert {} auto resolved by escalation", alertRecord.getId());
    }

    /**
     * 执行创建工单动作
     */
    private void executeCreateTicketAction(AlertEscalation escalation, AlertRecord alertRecord) {
        // 这里可以集成工单系统
        // 目前记录到备注中
        escalation.setResult("Ticket creation placeholder - no ticket system integrated");
        log.info("Ticket creation requested for alert {} (not implemented)", alertRecord.getId());
    }

    /**
     * 执行Webhook动作
     */
    private void executeWebhookAction(AlertEscalation escalation, AlertRecord alertRecord) {
        String webhookUrl = escalation.getTarget();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            webhookUrl = escalation.getNotificationConfig();
        }

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            throw new ServiceException("Webhook URL not configured");
        }

        // 构建Webhook载荷
        Map<String, Object> payload = new HashMap<>();
        payload.put("alertId", alertRecord.getId());
        payload.put("ruleId", alertRecord.getRuleId());
        payload.put("ruleName", alertRecord.getRuleName());
        payload.put("title", alertRecord.getTitle());
        payload.put("message", alertRecord.getMessage());
        payload.put("level", alertRecord.getLevel().name());
        payload.put("status", alertRecord.getStatus().name());
        payload.put("alertTime", alertRecord.getAlertTime().toString());
        payload.put("targetType", alertRecord.getTargetType() != null ? alertRecord.getTargetType().name() : null);
        payload.put("targetId", alertRecord.getTargetId());
        payload.put("targetName", alertRecord.getTargetName());
        payload.put("escalationLevel", escalation.getEscalationLevel());
        payload.put("escalationTime", LocalDateTime.now().toString());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(webhookUrl, request, String.class);
            
            log.info("Webhook executed for alert {}: {}", alertRecord.getId(), webhookUrl);
        } catch (Exception e) {
            throw new ServiceException("Webhook execution failed: " + e.getMessage());
        }
    }

    /**
     * 格式化模板
     */
    private String formatTemplate(String template, AlertRecord alertRecord) {
        if (template == null) {
            return alertRecord.getTitle();
        }

        return template
                .replace("{alertId}", String.valueOf(alertRecord.getId()))
                .replace("{title}", alertRecord.getTitle())
                .replace("{message}", alertRecord.getMessage() != null ? alertRecord.getMessage() : "")
                .replace("{level}", alertRecord.getLevel().name())
                .replace("{status}", alertRecord.getStatus().name())
                .replace("{alertTime}", alertRecord.getAlertTime().toString())
                .replace("{targetName}", alertRecord.getTargetName() != null ? alertRecord.getTargetName() : "");
    }

    /**
     * 获取额外配置
     */
    private Map<String, Object> getExtraConfig(AlertEscalation escalation) {
        Map<String, Object> config = new HashMap<>();
        if (escalation.getNotificationConfig() != null) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(
                        escalation.getNotificationConfig(), Map.class);
                config.putAll(parsed);
            } catch (Exception e) {
                log.warn("Failed to parse notification config: {}", e.getMessage());
            }
        }
        return config;
    }

    @Override
    public long countPendingEscalations() {
        return escalationRepository.findPendingEscalations().size();
    }
}
