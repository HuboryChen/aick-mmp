package com.aick.mmp.central.repository;

import com.aick.mmp.central.common.TestDataProvider;
import com.aick.mmp.shared.model.AlertRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AlertRuleRepositoryManualTest {

    @Autowired
    private AlertRuleRepository repository;

    @Autowired
    private TestDataProvider testDataProvider;

    @Test
    void testSaveAndRetrieveAlertRule() {
        // Given
        AlertRule rule = testDataProvider.createTestAlertRule();

        // When
        AlertRule saved = repository.save(rule);
        AlertRule retrieved = repository.findById(saved.getId()).orElse(null);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo(rule.getName());
        assertThat(retrieved.getAlertType()).isEqualTo(rule.getAlertType());
    }

    @Test
    void testFindEnabledAlertRules() {
        // Given
        AlertRule rule1 = testDataProvider.createTestAlertRule();
        AlertRule rule2 = testDataProvider.createTestAlertRule();
        rule2.setEnabled(false);

        repository.save(rule1);
        repository.save(rule2);

        // When
        List<AlertRule> enabledRules = repository.findByEnabledTrue();

        // Then
        assertThat(enabledRules).hasSize(1);
        assertThat(enabledRules.get(0).getName()).isEqualTo("Test Alert Rule");
    }

    @Test
    void testDeleteAlertRule() {
        // Given
        AlertRule rule = testDataProvider.createTestAlertRule();
        AlertRule saved = repository.save(rule);

        // When
        repository.delete(saved);

        // Then
        assertThat(repository.findById(saved.getId()).orElse(null)).isNull();
    }
}