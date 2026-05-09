package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.AlertNotification;
import com.aick.mmp.shared.model.NotificationSendLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知发送日志 Repository
 */
@Repository
public interface NotificationSendLogRepository extends JpaRepository<NotificationSendLog, Long> {

    /**
     * 根据告警记录ID查询发送日志
     */
    List<NotificationSendLog> findByAlertRecordIdOrderByCreatedAtDesc(Long alertRecordId);

    /**
     * 根据告警记录ID分页查询发送日志
     */
    Page<NotificationSendLog> findByAlertRecordId(Long alertRecordId, Pageable pageable);

    /**
     * 根据告警记录ID和渠道类型查询发送日志
     */
    List<NotificationSendLog> findByAlertRecordIdAndChannelType(Long alertRecordId, AlertNotification.ChannelType channelType);

    /**
     * 查询待发送的通知
     */
    List<NotificationSendLog> findByStatusOrderByPriorityDescCreatedAtAsc(NotificationSendLog.SendStatus status);

    /**
     * 查询需要重试的通知
     */
    @Query("SELECT n FROM NotificationSendLog n WHERE n.status = 'RETRYING' AND n.nextRetryAt <= :now")
    List<NotificationSendLog> findRetryableNotifications(@Param("now") LocalDateTime now);

    /**
     * 查询失败的通知（可重试且未超过最大次数）
     */
    @Query("SELECT n FROM NotificationSendLog n WHERE n.status = 'FAILED' AND n.retryable = true AND n.retryCount < n.maxRetry")
    List<NotificationSendLog> findFailedRetryableNotifications();

    /**
     * 查询指定时间范围内的发送日志
     */
    @Query("SELECT n FROM NotificationSendLog n WHERE n.createdAt >= :startTime AND n.createdAt <= :endTime ORDER BY n.createdAt DESC")
    List<NotificationSendLog> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各渠道发送成功率
     */
    @Query("SELECT n.channelType, COUNT(n), SUM(CASE WHEN n.status = 'SUCCESS' THEN 1 ELSE 0 END) FROM NotificationSendLog n " +
           "WHERE n.createdAt >= :startTime GROUP BY n.channelType")
    List<Object[]> countByChannelAndStatus(@Param("startTime") LocalDateTime startTime);

    /**
     * 统计指定告警的通知发送结果
     */
    @Query("SELECT n.status, COUNT(n) FROM NotificationSendLog n WHERE n.alertRecordId = :alertRecordId GROUP BY n.status")
    List<Object[]> countByAlertRecordIdGroupByStatus(@Param("alertRecordId") Long alertRecordId);

    /**
     * 删除指定时间之前的日志（用于清理历史数据）
     */
    void deleteByCreatedAtBefore(LocalDateTime beforeTime);

    /**
     * 查询发送失败次数最多的告警记录
     */
    @Query("SELECT n.alertRecordId, COUNT(n) FROM NotificationSendLog n WHERE n.status = 'FAILED' GROUP BY n.alertRecordId ORDER BY COUNT(n) DESC")
    List<Object[]> findMostFailedAlertRecords(Pageable pageable);
}
