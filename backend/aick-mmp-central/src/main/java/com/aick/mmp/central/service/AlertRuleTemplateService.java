package com.aick.mmp.central.service;

import com.aick.mmp.shared.model.AlertRule;
import com.aick.mmp.shared.model.AlertRuleTemplate;

import java.util.List;

/**
 * 告警规则模板服务接口
 */
public interface AlertRuleTemplateService {

    /**
     * 获取所有启用的模板
     */
    List<AlertRuleTemplate> getEnabledTemplates();

    /**
     * 根据ID获取模板
     */
    AlertRuleTemplate getTemplate(Long id);

    /**
     * 根据告警类型获取模板
     */
    List<AlertRuleTemplate> getTemplatesByAlertType(AlertRule.AlertType alertType);

    /**
     * 根据分类获取模板
     */
    List<AlertRuleTemplate> getTemplatesByCategory(AlertRuleTemplate.TemplateCategory category);

    /**
     * 获取推荐模板
     */
    List<AlertRuleTemplate> getRecommendedTemplates();

    /**
     * 搜索模板
     */
    List<AlertRuleTemplate> searchTemplates(String keyword);

    /**
     * 增加模板使用次数
     */
    void incrementUsageCount(Long templateId);
}
