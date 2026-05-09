package com.aick.mmp.central.repository;

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
 * 告警规则数据访问层
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long>, JpaSpecificationExecutor<AlertRule> {

    /**
     * 根据名称查找规则
     */
    Optional<AlertRule> findByName(String name);

    /**
     * 检查规则名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 根据告警类型查找规则
     */
    List<AlertRule> findByAlertType(AlertRule.AlertType alertType);

    /**
     * 根据告警类型分页查找启用的规则
     */
    Page<AlertRule> findByAlertTypeAndEnabled(AlertRule.AlertType alertType, Boolean enabled, Pageable pageable);

    /**
     * 根据级别查找规则
     */
    List<AlertRule> findByLevel(AlertRule.AlertLevel level);

    /**
     * 根据目标类型和目标ID查找规则
     */
    List<AlertRule> findByTargetTypeAndTargetId(AlertRule.TargetType targetType, Long targetId);

    /**
     * 查找所有启用的规则
     */
    List<AlertRule> findByEnabledTrue();

    /**
     * 根据状态查找规则
     */
    List<AlertRule> findByStatus(AlertRule.RuleStatus status);

    /**
     * 查找最近触发的规则
     */
    List<AlertRule> findByLastTriggeredAtAfter(LocalDateTime time);

    /**
     * 分页查找启用的规则
     */
    Page<AlertRule> findByEnabled(Boolean enabled, Pageable pageable);

    /**
     * 统计指定类型的告警规则数量
     */
    long countByAlertType(AlertRule.AlertType alertType);

    /**
     * 统计指定级别的告警规则数量
     */
    long countByLevel(AlertRule.AlertLevel level);

    /**
     * 统计启用状态的规则数量
     */
    long countByEnabled(Boolean enabled);

    /**
     * 更新规则状态
     */
    @Modifying
    @Query("UPDATE AlertRule r SET r.status = :status WHERE r.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") AlertRule.RuleStatus status);

    /**
     * 更新规则启用状态
     */
    @Modifying
    @Query("UPDATE AlertRule r SET r.enabled = :enabled WHERE r.id = :id")
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);

    /**
     * 更新最后触发时间
     */
    @Modifying
    @Query("UPDATE AlertRule r SET r.lastTriggeredAt = :lastTriggeredAt WHERE r.id = :id")
    int updateLastTriggeredAt(@Param("id") Long id, @Param("lastTriggeredAt") LocalDateTime lastTriggeredAt);

    /**
     * 根据目标类型查找启用的规则
     */
    List<AlertRule> findByTargetTypeAndEnabledTrue(AlertRule.TargetType targetType);
}
