package com.circleguard.form.service;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.ValidationStatus;
import com.circleguard.form.repository.HealthSurveyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HealthSurveyServiceTest {

    @Mock
    private HealthSurveyRepository repository;

    @Mock
    private QuestionnaireService questionnaireService;

    @Mock
    private SymptomMapper symptomMapper;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private HealthSurveyService healthSurveyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void submitSurvey_EmitsKafkaEvent() {
        HealthSurvey survey = new HealthSurvey();
        survey.setAnonymousId(UUID.randomUUID());
        survey.setHasFever(true);
        survey.setHasCough(false);

        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.empty());
        when(repository.save(any(HealthSurvey.class))).thenReturn(survey);

        HealthSurvey result = healthSurveyService.submitSurvey(survey);

        assertEquals(survey, result);
        verify(kafkaTemplate, times(1)).send(eq("survey.submitted"), eq(survey.getAnonymousId().toString()), any());
    }

    @Test
    void submitSurvey_WithAttachment_SetsPendingStatus() {
        HealthSurvey survey = new HealthSurvey();
        survey.setAnonymousId(UUID.randomUUID());
        survey.setAttachmentPath("/path/to/doc.pdf");

        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.empty());
        when(repository.save(any(HealthSurvey.class))).thenReturn(survey);

        healthSurveyService.submitSurvey(survey);

        assertEquals(ValidationStatus.PENDING, survey.getValidationStatus());
    }

    @Test
    void getPendingSurveys_ReturnsList() {
        when(repository.findByAttachmentPathIsNotNullAndValidationStatus(ValidationStatus.PENDING))
                .thenReturn(Collections.singletonList(new HealthSurvey()));

        List<HealthSurvey> pending = healthSurveyService.getPendingSurveys();

        assertFalse(pending.isEmpty());
        assertEquals(1, pending.size());
    }

    @Test
    void validateSurvey_Approved_EmitsKafkaEvent() {
        UUID surveyId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        HealthSurvey survey = new HealthSurvey();
        survey.setId(surveyId);
        survey.setAnonymousId(UUID.randomUUID());

        when(repository.findById(surveyId)).thenReturn(Optional.of(survey));

        healthSurveyService.validateSurvey(surveyId, ValidationStatus.APPROVED, adminId);

        assertEquals(ValidationStatus.APPROVED, survey.getValidationStatus());
        assertEquals(adminId, survey.getValidatedBy());
        verify(kafkaTemplate, times(1)).send(eq("certificate.validated"), eq(survey.getAnonymousId().toString()), any());
        verify(repository, times(1)).save(survey);
    }

    @Test
    void validateSurvey_Rejected_DoesNotEmitEvent() {
        UUID surveyId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        HealthSurvey survey = new HealthSurvey();
        survey.setId(surveyId);

        when(repository.findById(surveyId)).thenReturn(Optional.of(survey));

        healthSurveyService.validateSurvey(surveyId, ValidationStatus.REJECTED, adminId);

        assertEquals(ValidationStatus.REJECTED, survey.getValidationStatus());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
