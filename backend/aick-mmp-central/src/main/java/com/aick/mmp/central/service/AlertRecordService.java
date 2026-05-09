package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.AlertStatistics;
import com.aick.mmp.shared.model.AlertRecord;
import com.aick.mmp.shared.model.AlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警记录服务接口
 */
public interface AlertRecordService {

    /**
     * 创建告警记录
     */
    AlertRecord createAlert(AlertRule rule, AlertRule.AlertLevel level, 
                             String title, String message, Long targetId, String targetName,
                             Double actualValue, Double thresholdValue);

    /**
     * 获取告警记录详情
     */
    Optional<AlertRecord> getAlert(Long id);

    /**
     * 分页查询告警记录
     */
    Page<AlertRecord> listAlerts(Pageable pageable);

    /**
     * 根据规则ID查询告警
     */
    Page<AlertRecord> findByRuleId(Long ruleId, Pageable pageable);

    /**
     * 根据级别查询告警
     */
    Page<AlertRecord> findByLevel(AlertRule.AlertLevel level, Pageable pageable);

    /**
     * 根据状态查询告警
     */
    Page<AlertRecord> findByStatus(AlertRecord.AlertStatus status, Pageable pageable);

    /**
     * 根据摄像头ID查询告警
     */
    Page<AlertRecord> findByCameraId(Long cameraId, Pageable pageable);

    /**
     * 根据时间范围查询告警
     */
    Page<AlertRecord> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 确认告警
     */
    void acknowledgeAlert(Long id, Long userId, String username);

    /**
     * 处理告警
     */
    void resolveAlert(Long id, Long userId, String username, String resolutionNote);

    /**
     * 批量处理告警
     */
    void batchResolveAlerts(List<Long> ids, Long userId, String username, String resolutionNote);

    /**
     * 获取未处理的告警
     */
    List<AlertRecord> getUnresolvedAlerts();

    /**
     * 获取最新告警
     */
    List<AlertRecord> getRecentAlerts(int limit);

    /**
     * 获取今日告警
     */
    List<AlertRecord> getTodayAlerts();

    /**
     * 获取告警统计
     */
    AlertStatistics getStatistics();

    /**
     * 获取告警统计（按时间范围）
     */
    AlertStatistics getStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 检查是否在冷却期
     */
    boolean isInCooldown(Long ruleId);
}
