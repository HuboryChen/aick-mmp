package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.AlertRule;
import com.aick.mmp.shared.model.AlertRuleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 告警规则模板数据访问层
 */
@Repository
public interface AlertRuleTemplateRepository extends JpaRepository<AlertRuleTemplate, Long>, JpaSpecificationExecutor<AlertRuleTemplate> {

    /**
     * 根据名称查找模板
     */
    Optional<AlertRuleTemplate> findByName(String name);

    /**
     * 检查模板名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 根据分类查找模板
     */
    List<AlertRuleTemplate> findByCategory(AlertRuleTemplate.TemplateCategory category);

    /**
     * 根据告警类型查找模板
     */
    List<AlertRuleTemplate> findByAlertType(AlertRule.AlertType alertType);

    /**
     * 根据告警类型查找启用的模板
     */
    List<AlertRuleTemplate> findByAlertTypeAndIsEnabledTrue(AlertRule.AlertType alertType);

    /**
     * 查找所有启用的模板
     */
    List<AlertRuleTemplate> findByIsEnabledTrue();

    /**
     * 查找系统内置模板
     */
    List<AlertRuleTemplate> findByIsSystemTrue();

    /**
     * 查找用户自定义模板
     */
    List<AlertRuleTemplate> findByCreatedBy(Long createdBy);

    /**
     * 根据标签查找模板
     */
    @Query("SELECT t FROM AlertRuleTemplate t WHERE t.tags LIKE %:tag%")
    List<AlertRuleTemplate> findByTag(@Param("tag") String tag);

    /**
     * 根据分类和告警类型查找模板
     */
    List<AlertRuleTemplate> findByCategoryAndAlertType(
            AlertRuleTemplate.TemplateCategory category,
            AlertRule.AlertType alertType);

    /**
     * 查找热门模板（按使用次数排序）
     */
    List<AlertRuleTemplate> findByIsEnabledTrueOrderByUsageCountDesc();

    /**
     * 搜索模板（名称或描述）
     */
    @Query("SELECT t FROM AlertRuleTemplate t WHERE " +
           "t.name LIKE %:keyword% OR t.description LIKE %:keyword%")
    List<AlertRuleTemplate> search(@Param("keyword") String keyword);

    /**
     * 统计分类下的模板数量
     */
    long countByCategory(AlertRuleTemplate.TemplateCategory category);

    /**
     * 统计指定告警类型的模板数量
     */
    long countByAlertType(AlertRule.AlertType alertType);

    /**
     * 增加使用次数
     */
    @Modifying
    @Query("UPDATE AlertRuleTemplate t SET t.usageCount = t.usageCount + 1 WHERE t.id = :id")
    int incrementUsageCount(@Param("id") Long id);

    /**
     * 更新启用状态
     */
    @Modifying
    @Query("UPDATE AlertRuleTemplate t SET t.isEnabled = :enabled WHERE t.id = :id")
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);

    /**
     * 查找推荐模板
     */
    @Query("SELECT t FROM AlertRuleTemplate t WHERE t.isEnabled = true " +
           "AND (t.isSystem = true OR t.usageCount > 0) " +
           "ORDER BY t.isSystem DESC, t.usageCount DESC")
    List<AlertRuleTemplate> findRecommendedTemplates();
}
