package com.aick.mmp.central.repository;

import com.aick.mmp.central.common.BaseIntegrationTest;
import com.aick.mmp.central.common.TestDataProvider;
import com.aick.mmp.shared.model.AlertRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import org.junit.jupiter.api.DisplayName;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class AlertRuleRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private AlertRuleRepository repository;

    @Autowired
    private TestDataProvider testDataProvider;

    @Test
    @DisplayName("should save and retrieve alert rule")
    void shouldSaveAndRetrieveAlertRule() {
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
    @DisplayName("should find enabled alert rules")
    void shouldFindEnabledAlertRules() {
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
    @DisplayName("should delete alert rule")
    void shouldDeleteAlertRule() {
        // Given
        AlertRule rule = testDataProvider.createTestAlertRule();
        AlertRule saved = repository.save(rule);

        // When
        repository.delete(saved);

        // Then
        assertThat(repository.findById(saved.getId()).orElse(null)).isNull();
    }
}