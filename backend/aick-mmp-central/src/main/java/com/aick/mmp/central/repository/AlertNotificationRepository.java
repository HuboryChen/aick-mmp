package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.AlertNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 告警通知配置数据访问层
 */
@Repository
public interface AlertNotificationRepository extends JpaRepository<AlertNotification, Long> {

    /**
     * 根据规则ID查找所有通知配置
     */
    List<AlertNotification> findByRuleId(Long ruleId);

    /**
     * 根据告警记录ID查找通知配置
     */
    List<AlertNotification> findByAlertRecordId(Long alertRecordId);

    /**
     * 根据规则ID和启用状态查找
     */
    List<AlertNotification> findByRuleIdAndIsEnabled(Long ruleId, Boolean isEnabled);

    /**
     * 根据规则ID删除所有通知配置
     */
    void deleteByRuleId(Long ruleId);
}
