package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.AlertRecord;
import com.aick.mmp.shared.model.AlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警记录数据访问层
 */
@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long>, JpaSpecificationExecutor<AlertRecord> {

    /**
     * 根据规则ID查找告警记录
     */
    Page<AlertRecord> findByRuleId(Long ruleId, Pageable pageable);
    
    /**
     * 根据规则ID查找告警记录
     */
    List<AlertRecord> findByRuleId(Long ruleId);

    /**
     * 根据告警级别查找告警记录
     */
    Page<AlertRecord> findByLevel(AlertRule.AlertLevel level, Pageable pageable);

    /**
     * 根据告警状态查找告警记录
     */
    Page<AlertRecord> findByStatus(AlertRecord.AlertStatus status, Pageable pageable);

    /**
     * 根据摄像头ID查找告警记录
     */
    Page<AlertRecord> findByCameraId(Long cameraId, Pageable pageable);

    /**
     * 根据边缘节点ID查找告警记录
     */
    Page<AlertRecord> findByEdgeNodeId(Long edgeNodeId, Pageable pageable);

    /**
     * 根据时间范围查找告警记录
     */
    Page<AlertRecord> findByAlertTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据时间范围查找所有告警记录
     */
    List<AlertRecord> findByAlertTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找未处理的告警记录
     */
    List<AlertRecord> findByStatus(AlertRecord.AlertStatus status);

    /**
     * 查找指定时间后的告警记录
     */
    List<AlertRecord> findByAlertTimeAfter(LocalDateTime time);

    /**
     * 查找指定时间后且指定状态的告警记录
     */
    List<AlertRecord> findByAlertTimeAfterAndStatus(LocalDateTime time, AlertRecord.AlertStatus status);

    /**
     * 查找指定规则且指定时间后的告警记录
     */
    List<AlertRecord> findByRuleIdAndAlertTimeAfter(Long ruleId, LocalDateTime time);

    /**
     * 统计指定规则且指定时间后的告警记录数量
     */
    long countByRuleIdAndAlertTimeAfter(Long ruleId, LocalDateTime time);

    /**
     * 查找指定规则的最后告警记录
     */
    Optional<AlertRecord> findTopByRuleIdOrderByAlertTimeDesc(Long ruleId);

    /**
     * 查找指定时间范围内的未处理告警
     */
    List<AlertRecord> findByAlertTimeBetweenAndStatus(
            LocalDateTime startTime, LocalDateTime endTime, AlertRecord.AlertStatus status);

    /**
     * 统计指定状态的告警数量
     */
    long countByStatus(AlertRecord.AlertStatus status);

    /**
     * 统计指定规则的告警数量
     */
    long countByRuleId(Long ruleId);

    /**
     * 统计指定级别的告警数量
     */
    long countByLevel(AlertRule.AlertLevel level);

    /**
     * 统计指定时间范围内的告警数量
     */
    long countByAlertTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定时间范围内的指定级别告警数量
     */
    @Query("SELECT COUNT(r) FROM AlertRecord r " +
           "WHERE r.alertTime >= :startTime AND r.alertTime <= :endTime AND r.level = :level")
    long countByAlertTimeBetweenAndLevel(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("level") AlertRule.AlertLevel level);

    /**
     * 统计指定摄像头未处理的告警数量
     */
    long countByCameraIdAndStatus(Long cameraId, AlertRecord.AlertStatus status);

    /**
     * 统计指定边缘节点未处理的告警数量
     */
    long countByEdgeNodeIdAndStatus(Long edgeNodeId, AlertRecord.AlertStatus status);

    /**
     * 查找最新告警
     */
    List<AlertRecord> findTop10ByOrderByAlertTimeDesc();

    /**
     * 根据级别和时间范围统计
     */
    @Query("SELECT r.level, COUNT(r) FROM AlertRecord r " +
           "WHERE r.alertTime >= :startTime AND r.alertTime <= :endTime " +
           "GROUP BY r.level")
    List<Object[]> countByLevelAndTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的指定级别告警数量
     */
    @Query("SELECT COUNT(r) FROM AlertRecord r " +
           "WHERE r.alertTime >= :startTime AND r.alertTime <= :endTime AND r.level = :level")
    long countByLevelAndTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("level") AlertRule.AlertLevel level);

    /**
     * 根据告警类型和时间范围统计
     */
    @Query("SELECT r.alertType, COUNT(r) FROM AlertRecord r " +
           "WHERE r.alertTime >= :startTime AND r.alertTime <= :endTime " +
           "GROUP BY r.alertType")
    List<Object[]> countByAlertTypeAndTimeRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 更新告警状态
     */
    @Modifying
    @Query("UPDATE AlertRecord r SET r.status = :status WHERE r.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") AlertRecord.AlertStatus status);

    /**
     * 批量更新告警状态
     */
    @Modifying
    @Query("UPDATE AlertRecord r SET r.status = :status WHERE r.id IN :ids")
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") AlertRecord.AlertStatus status);

    /**
     * 解决告警
     */
    @Modifying
    @Query("UPDATE AlertRecord r SET r.status = :status, r.resolvedBy = :resolvedBy, " +
           "r.resolvedByUsername = :resolvedByUsername, r.resolvedAt = :resolvedAt, " +
           "r.resolutionNote = :resolutionNote WHERE r.id = :id")
    int resolveAlert(@Param("id") Long id,
                      @Param("status") AlertRecord.AlertStatus status,
                      @Param("resolvedBy") Long resolvedBy,
                      @Param("resolvedByUsername") String resolvedByUsername,
                      @Param("resolvedAt") LocalDateTime resolvedAt,
                      @Param("resolutionNote") String resolutionNote);

    /**
     * 确认告警
     */
    @Modifying
    @Query("UPDATE AlertRecord r SET r.status = 'ACKNOWLEDGED', " +
           "r.acknowledgedAt = :acknowledgedAt, r.acknowledgedBy = :acknowledgedBy, " +
           "r.acknowledgedByUsername = :acknowledgedByUsername WHERE r.id = :id")
    int acknowledgeAlert(@Param("id") Long id,
                         @Param("acknowledgedAt") LocalDateTime acknowledgedAt,
                         @Param("acknowledgedBy") Long acknowledgedBy,
                         @Param("acknowledgedByUsername") String acknowledgedByUsername);

    /**
     * 更新通知状态
     */
    @Modifying
    @Query("UPDATE AlertRecord r SET r.notificationSent = true, " +
           "r.notificationSentAt = :sentAt WHERE r.id = :id")
    int updateNotificationSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    /**
     * 分页查找告警记录（支持多条件筛选）
     */
    Page<AlertRecord> findByAlertTimeAfterAndLevelAndStatus(
            LocalDateTime alertTime, AlertRule.AlertLevel level, AlertRecord.AlertStatus status, Pageable pageable);

    /**
     * 查找今日告警
     */
    @Query("SELECT r FROM AlertRecord r WHERE r.alertTime >= :todayStart ORDER BY r.alertTime DESC")
    List<AlertRecord> findTodayAlerts(@Param("todayStart") LocalDateTime todayStart);

    /**
     * 删除指定规则的所有告警记录
     */
    @Modifying
    void deleteByRuleId(Long ruleId);
}
