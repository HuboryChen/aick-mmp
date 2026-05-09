package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.repository.AlertRuleTemplateRepository;
import com.aick.mmp.central.service.AlertRuleTemplateService;
import com.aick.mmp.shared.model.AlertRule;
import com.aick.mmp.shared.model.AlertRuleTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 告警规则模板服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AlertRuleTemplateServiceImpl implements AlertRuleTemplateService {

    private final AlertRuleTemplateRepository templateRepository;

    @Override
    public List<AlertRuleTemplate> getEnabledTemplates() {
        return templateRepository.findByIsEnabledTrue();
    }

    @Override
    public AlertRuleTemplate getTemplate(Long id) {
        return templateRepository.findById(id).orElse(null);
    }

    @Override
    public List<AlertRuleTemplate> getTemplatesByAlertType(AlertRule.AlertType alertType) {
        return templateRepository.findByAlertTypeAndIsEnabledTrue(alertType);
    }

    @Override
    public List<AlertRuleTemplate> getTemplatesByCategory(AlertRuleTemplate.TemplateCategory category) {
        return templateRepository.findByCategory(category);
    }

    @Override
    public List<AlertRuleTemplate> getRecommendedTemplates() {
        return templateRepository.findRecommendedTemplates();
    }

    @Override
    public List<AlertRuleTemplate> searchTemplates(String keyword) {
        return templateRepository.search(keyword);
    }

    @Override
    @Transactional
    public void incrementUsageCount(Long templateId) {
        templateRepository.incrementUsageCount(templateId);
    }
}
