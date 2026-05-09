package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.AlertEscalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警升级配置数据访问层
 */
@Repository
public interface AlertEscalationRepository extends JpaRepository<AlertEscalation, Long> {

    /**
     * 根据告警记录ID查找所有升级配置
     */
    List<AlertEscalation> findByAlertRecordId(Long alertRecordId);

    /**
     * 根据规则ID查找所有升级配置
     */
    List<AlertEscalation> findByRuleId(Long ruleId);

    /**
     * 查找待执行的升级配置
     */
    List<AlertEscalation> findByStatus(AlertEscalation.EscalationStatus status);

    /**
     * 查找未触发的升级配置
     */
    List<AlertEscalation> findByIsTriggeredFalse();

    /**
     * 根据告警记录ID和升级级别查找
     */
    List<AlertEscalation> findByAlertRecordIdAndEscalationLevel(Long alertRecordId, Integer escalationLevel);

    /**
     * 查找已触发的升级配置
     */
    List<AlertEscalation> findByAlertRecordIdAndIsTriggeredTrue(Long alertRecordId);

    /**
     * 更新升级状态
     */
    @Modifying
    @Query("UPDATE AlertEscalation e SET e.status = :status, e.result = :result WHERE e.id = :id")
    int updateStatus(@Param("id") Long id, 
                     @Param("status") AlertEscalation.EscalationStatus status,
                     @Param("result") String result);

    /**
     * 标记升级已触发
     */
    @Modifying
    @Query("UPDATE AlertEscalation e SET e.isTriggered = true, e.triggeredAt = :triggeredAt, " +
           "e.status = :status WHERE e.id = :id")
    int markAsTriggered(@Param("id") Long id,
                         @Param("triggeredAt") LocalDateTime triggeredAt,
                         @Param("status") AlertEscalation.EscalationStatus status);

    /**
     * 根据告警记录ID删除所有升级配置
     */
    @Modifying
    void deleteByAlertRecordId(Long alertRecordId);

    /**
     * 根据规则ID删除所有升级配置
     */
    @Modifying
    void deleteByRuleId(Long ruleId);

    /**
     * 查找需要检查的升级配置（未触发且状态为待执行）
     */
    @Query("SELECT e FROM AlertEscalation e WHERE e.isTriggered = false " +
           "AND e.status = 'PENDING' ORDER BY e.alertRecordId, e.escalationLevel")
    List<AlertEscalation> findPendingEscalations();
}
