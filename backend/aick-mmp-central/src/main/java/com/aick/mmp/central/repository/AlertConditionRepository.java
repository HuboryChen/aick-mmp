package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.AlertCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 告警条件数据访问层
 */
@Repository
public interface AlertConditionRepository extends JpaRepository<AlertCondition, Long> {

    /**
     * 根据规则ID查找所有条件
     */
    List<AlertCondition> findByRuleId(Long ruleId);

    /**
     * 根据规则ID查找启用的条件
     */
    List<AlertCondition> findByRuleIdAndIsEnabledTrue(Long ruleId);

    /**
     * 根据父条件ID查找子条件
     */
    List<AlertCondition> findByParentConditionId(Long parentConditionId);

    /**
     * 根据规则ID和条件类型查找条件
     */
    List<AlertCondition> findByRuleIdAndConditionType(Long ruleId, AlertCondition.ConditionType conditionType);

    /**
     * 根据规则ID删除所有条件
     */
    @Modifying
    void deleteByRuleId(Long ruleId);

    /**
     * 批量删除条件
     */
    @Modifying
    void deleteByIdIn(List<Long> ids);

    /**
     * 统计规则的条件数量
     */
    long countByRuleId(Long ruleId);

    /**
     * 批量更新启用状态
     */
    @Modifying
    @Query("UPDATE AlertCondition c SET c.isEnabled = :enabled WHERE c.id IN :ids")
    int batchUpdateEnabled(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled);

    /**
     * 查找规则的顶层条件（无父条件）
     */
    List<AlertCondition> findByRuleIdAndParentConditionIdIsNull(Long ruleId);
}
