package com.circleguard.form.integration;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.ValidationStatus;
import com.circleguard.form.repository.HealthSurveyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class HealthSurveyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("form_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private HealthSurveyRepository repository;

    @Test
    void contextLoads() {
        assertTrue(postgres.isRunning());
    }

    @Test
    void saveAndFindById() {
        HealthSurvey survey = new HealthSurvey();
        survey.setAnonymousId(UUID.randomUUID());
        survey.setHasFever(true);
        HealthSurvey saved = repository.save(survey);

        HealthSurvey found = repository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals(survey.getAnonymousId(), found.getAnonymousId());
    }

    @Test
    void findByAttachmentPathIsNotNullAndValidationStatus() {
        HealthSurvey survey = new HealthSurvey();
        survey.setAnonymousId(UUID.randomUUID());
        survey.setAttachmentPath("/docs/medical.pdf");
        survey.setValidationStatus(ValidationStatus.PENDING);
        repository.save(survey);

        List<HealthSurvey> results = repository.findByAttachmentPathIsNotNullAndValidationStatus(ValidationStatus.PENDING);
        assertTrue(results.size() > 0);
    }

    @Test
    void deleteSurvey() {
        HealthSurvey survey = new HealthSurvey();
        survey.setAnonymousId(UUID.randomUUID());
        HealthSurvey saved = repository.save(survey);

        repository.deleteById(saved.getId());
        assertTrue(repository.findById(saved.getId()).isEmpty());
    }

    @Test
    void updateSurveyStatus() {
        HealthSurvey survey = new HealthSurvey();
        survey.setAnonymousId(UUID.randomUUID());
        survey.setValidationStatus(ValidationStatus.PENDING);
        HealthSurvey saved = repository.save(survey);

        saved.setValidationStatus(ValidationStatus.APPROVED);
        repository.save(saved);

        HealthSurvey updated = repository.findById(saved.getId()).get();
        assertEquals(ValidationStatus.APPROVED, updated.getValidationStatus());
    }
}
