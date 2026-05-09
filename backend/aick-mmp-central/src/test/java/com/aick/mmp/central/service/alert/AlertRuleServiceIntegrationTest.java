package com.aick.mmp.central.service.alert;

import com.aick.mmp.central.common.BaseIntegrationTest;
import com.aick.mmp.central.common.TestDataProvider;
import com.aick.mmp.central.dto.AlertRuleDTO;
import com.aick.mmp.shared.model.AlertRule;
import com.aick.mmp.central.repository.AlertRuleRepository;
import com.aick.mmp.central.service.AlertRuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.transaction.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.aick.mmp.central.CentralApplication.class)
@Transactional
public class AlertRuleServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private AlertRuleRepository alertRuleRepository;

    @Autowired
    private TestDataProvider testDataProvider;

    @Test
    @DisplayName("should create alert rule with valid data")
    void shouldCreateAlertRuleWithValidData() {
        // Given
        AlertRuleDTO request = new AlertRuleDTO();
        request.setName("New Alert Rule");
        request.setType("THRESHOLD");
        request.setEnabled(true);
        request.setThresholdValue(90);

        // When
        AlertRuleDTO result = alertRuleService.createAlertRule(request);

        // Then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("New Alert Rule");
        assertThat(result.getType()).isEqualTo("THRESHOLD");
        assertThat(result.getEnabled()).isTrue();
        assertThat(result.getThresholdValue()).isEqualTo(90);

        // Verify database state
        AlertRule saved = alertRuleRepository.findById(result.getId()).orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("should update alert rule")
    void shouldUpdateAlertRule() {
        // Given
        AlertRule existing = alertRuleRepository.save(testDataProvider.createTestAlertRule());
        AlertRuleDTO updateRequest = new AlertRuleDTO();
        updateRequest.setName("Updated Name");
        updateRequest.setEnabled(false);

        // When
        AlertRuleDTO result = alertRuleService.updateAlertRule(existing.getId(), updateRequest);

        // Then
        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getEnabled()).isFalse();
    }
}