package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.AlertRecordRequest;
import com.aick.mmp.central.dto.AlertStatistics;
import com.aick.mmp.central.repository.AlertRecordRepository;
import com.aick.mmp.central.repository.AlertRuleRepository;
import com.aick.mmp.central.service.AlertRecordService;
import com.aick.mmp.central.service.AlertNotificationService;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.AlertRecord;
import com.aick.mmp.shared.model.AlertRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 告警记录服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AlertRecordServiceImpl implements AlertRecordService {

    private final AlertRecordRepository alertRecordRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertNotificationService notificationService;

    @Override
    public AlertRecord createAlert(AlertRule rule, AlertRule.AlertLevel level,
                                   String title, String message, Long targetId, String targetName,
                                   Double actualValue, Double thresholdValue) {
        // 检查冷却期
        if (isInCooldown(rule.getId())) {
            log.info("Alert rule {} is in cooldown, skipping alert creation", rule.getId());
            return null;
        }

        AlertRecord record = AlertRecord.builder()
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .alertType(rule.getAlertType())
                .level(level)
                .title(title)
                .message(message)
                .alertTime(LocalDateTime.now())
                .status(AlertRecord.AlertStatus.UNRESOLVED)
                .targetType(rule.getTargetType())
                .targetId(targetId)
                .targetName(targetName)
                .actualValue(actualValue)
                .thresholdValue(thresholdValue)
                .notificationSent(false)
                .build();

        AlertRecord saved = alertRecordRepository.save(record);

        // 更新规则的触发时间
        alertRuleRepository.updateLastTriggeredAt(rule.getId(), LocalDateTime.now());

        // 发送通知
        try {
            notificationService.sendAlertNotification(saved);
            saved.setNotificationSent(true);
            saved.setNotificationSentAt(LocalDateTime.now());
            alertRecordRepository.save(saved);
        } catch (Exception e) {
            log.error("Failed to send alert notification for record {}: {}", saved.getId(), e.getMessage());
        }

        log.info("Created alert record: {} for rule {}", saved.getId(), rule.getName());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AlertRecord> getAlert(Long id) {
        return alertRecordRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertRecord> listAlerts(Pageable pageable) {
        return alertRecordRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertRecord> findByRuleId(Long ruleId, Pageable pageable) {
        return alertRecordRepository.findByRuleId(ruleId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertRecord> findByLevel(AlertRule.AlertLevel level, Pageable pageable) {
        return alertRecordRepository.findByLevel(level, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertRecord> findByStatus(AlertRecord.AlertStatus status, Pageable pageable) {
        return alertRecordRepository.findByStatus(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertRecord> findByCameraId(Long cameraId, Pageable pageable) {
        return alertRecordRepository.findByCameraId(cameraId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertRecord> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return alertRecordRepository.findByAlertTimeBetween(startTime, endTime, pageable);
    }

    @Override
    public void acknowledgeAlert(Long id, Long userId, String username) {
        AlertRecord record = alertRecordRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Alert record not found: " + id));
        
        record.setStatus(AlertRecord.AlertStatus.ACKNOWLEDGED);
        record.setAcknowledgedAt(LocalDateTime.now());
        record.setAcknowledgedBy(userId);
        record.setAcknowledgedByUsername(username);
        
        alertRecordRepository.save(record);
        log.info("Alert {} acknowledged by {}", id, username);
    }

    @Override
    public void resolveAlert(Long id, Long userId, String username, String resolutionNote) {
        AlertRecord record = alertRecordRepository.findById(id)
                .orElseThrow(() -> new ServiceException("Alert record not found: " + id));
        
        record.setStatus(AlertRecord.AlertStatus.RESOLVED);
        record.setResolvedBy(userId);
        record.setResolvedByUsername(username);
        record.setResolvedAt(LocalDateTime.now());
        record.setResolutionNote(resolutionNote);
        
        alertRecordRepository.save(record);
        log.info("Alert {} resolved by {}", id, username);
    }

    @Override
    public void batchResolveAlerts(List<Long> ids, Long userId, String username, String resolutionNote) {
        LocalDateTime now = LocalDateTime.now();
        for (Long id : ids) {
            alertRecordRepository.resolveAlert(
                    id,
                    AlertRecord.AlertStatus.RESOLVED,
                    userId,
                    username,
                    now,
                    resolutionNote
            );
        }
        log.info("Batch resolved {} alerts by {}", ids.size(), username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertRecord> getUnresolvedAlerts() {
        return alertRecordRepository.findByStatus(AlertRecord.AlertStatus.UNRESOLVED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertRecord> getRecentAlerts(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "alertTime"));
        return alertRecordRepository.findAll(pageable).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertRecord> getTodayAlerts() {
        LocalDateTime todayStart = LocalDateTime.now().with(LocalTime.MIN);
        return alertRecordRepository.findTodayAlerts(todayStart);
    }

    @Override
    public AlertStatistics getStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.with(LocalTime.MIN);
        LocalDateTime weekStart = now.minusDays(7);
        
        return buildStatistics(weekStart, now, todayStart);
    }

    @Override
    public AlertStatistics getStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime todayStart = LocalDateTime.now().with(LocalTime.MIN);
        return buildStatistics(startTime, endTime, todayStart);
    }

    private AlertStatistics buildStatistics(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime todayStart) {
        long totalCount = alertRecordRepository.countByAlertTimeBetween(startTime, endTime);
        long unresolvedCount = alertRecordRepository.countByStatus(AlertRecord.AlertStatus.UNRESOLVED);
        long acknowledgedCount = alertRecordRepository.countByStatus(AlertRecord.AlertStatus.ACKNOWLEDGED);
        long todayCount = alertRecordRepository.countByAlertTimeBetween(todayStart, LocalDateTime.now());
        
        long criticalCount = alertRecordRepository.countByAlertTimeBetweenAndLevel(startTime, endTime, 
                AlertRule.AlertLevel.CRITICAL);
        long warningCount = alertRecordRepository.countByAlertTimeBetweenAndLevel(startTime, endTime, 
                AlertRule.AlertLevel.WARNING);
        long infoCount = alertRecordRepository.countByAlertTimeBetweenAndLevel(startTime, endTime, 
                AlertRule.AlertLevel.INFO);

        // 按级别统计
        Map<String, Long> countByLevel = new HashMap<>();
        List<Object[]> levelStats = alertRecordRepository.countByLevelAndTimeRange(startTime, endTime);
        for (Object[] stat : levelStats) {
            countByLevel.put(stat[0].toString(), (Long) stat[1]);
        }

        // 按类型统计
        Map<String, Long> countByType = new HashMap<>();
        List<Object[]> typeStats = alertRecordRepository.countByAlertTypeAndTimeRange(startTime, endTime);
        for (Object[] stat : typeStats) {
            countByType.put(stat[0].toString(), (Long) stat[1]);
        }

        // 按状态统计
        Map<String, Long> countByStatus = new HashMap<>();
        countByStatus.put("UNRESOLVED", unresolvedCount);
        countByStatus.put("ACKNOWLEDGED", acknowledgedCount);
        countByStatus.put("RESOLVED", alertRecordRepository.countByStatus(AlertRecord.AlertStatus.RESOLVED));

        return AlertStatistics.builder()
                .totalCount(totalCount)
                .unresolvedCount(unresolvedCount)
                .acknowledgedCount(acknowledgedCount)
                .todayCount(todayCount)
                .criticalCount(criticalCount)
                .warningCount(warningCount)
                .infoCount(infoCount)
                .countByLevel(countByLevel)
                .countByType(countByType)
                .countByStatus(countByStatus)
                .build();
    }

    @Override
    public boolean isInCooldown(Long ruleId) {
        Optional<AlertRule> ruleOpt = alertRuleRepository.findById(ruleId);
        if (ruleOpt.isEmpty()) {
            return false;
        }

        AlertRule rule = ruleOpt.get();
        LocalDateTime cooldownStart = LocalDateTime.now().minusSeconds(rule.getCooldownSeconds());

        List<AlertRecord> recentAlerts = alertRecordRepository.findByRuleIdAndAlertTimeAfter(
                ruleId, cooldownStart);
        
        return !recentAlerts.isEmpty();
    }
    
    private List<AlertRecord> findByRuleIdAndAlertTimeAfter(Long ruleId, LocalDateTime time) {
        return alertRecordRepository.findByAlertTimeAfterAndStatus(time, AlertRecord.AlertStatus.UNRESOLVED);
    }
}
