package com.streamguard.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.streamguard.common.model.*;
import com.streamguard.validation.metrics.ValidationMetrics;
import com.streamguard.validation.persistence.ViolationRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidationServiceTest {

    private ContractCacheService contractCache;
    private ViolationRepository violationRepository;
    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        contractCache = mock(ContractCacheService.class);
        violationRepository = mock(ViolationRepository.class);
        ValidationMetrics metrics = new ValidationMetrics(new SimpleMeterRegistry());
        metrics.init();
        validationService = new ValidationService(
                contractCache,
                new QualityRuleEvaluator(),
                violationRepository,
                metrics);
    }

    @Test
    void detectsMissingRequiredField() {
        LinkedHashMap<String, FieldDefinition> fields = new LinkedHashMap<>();
        fields.put("amount", new FieldDefinition(FieldType.DECIMAL, true));
        when(contractCache.getContract("ORDER_CREATED", 1))
                .thenReturn(new DataContract("ORDER_CREATED", 1, fields, List.of()));

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        ValidationResult result = validationService.validate(
                new EventEnvelope("ORDER_CREATED", 1, "id-1", payload));

        assertThat(result.isValid()).isFalse();
        assertThat(result.violations())
                .anyMatch(v -> v.violationType().equals("MISSING_FIELD"));
    }

    @Test
    void detectsTypeMismatch() {
        LinkedHashMap<String, FieldDefinition> fields = new LinkedHashMap<>();
        fields.put("amount", new FieldDefinition(FieldType.DECIMAL, true));
        when(contractCache.getContract("ORDER_CREATED", 1))
                .thenReturn(new DataContract("ORDER_CREATED", 1, fields, List.of()));

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("amount", "oops-a-string");
        ValidationResult result = validationService.validate(
                new EventEnvelope("ORDER_CREATED", 1, "id-2", payload));

        assertThat(result.violations())
                .anyMatch(v -> v.violationType().equals("TYPE_MISMATCH"));
    }

    @Test
    void detectsQualityRuleFailure() {
        LinkedHashMap<String, FieldDefinition> fields = new LinkedHashMap<>();
        fields.put("amount", new FieldDefinition(FieldType.DECIMAL, true));
        var contract = new DataContract("ORDER_CREATED", 1, fields,
                List.of(new QualityRule("amount", "GREATER_THAN", "0")));
        when(contractCache.getContract("ORDER_CREATED", 1)).thenReturn(contract);

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("amount", -3.5);

        ValidationResult result = validationService.validate(
                new EventEnvelope("ORDER_CREATED", 1, "id-3", payload));

        assertThat(result.violations())
                .anyMatch(v -> v.violationType().equals("RULE_FAILURE"));
    }

    @Test
    void acceptsValidPayload() {
        LinkedHashMap<String, FieldDefinition> fields = new LinkedHashMap<>();
        fields.put("amount", new FieldDefinition(FieldType.DECIMAL, true));
        when(contractCache.getContract("ORDER_CREATED", 1))
                .thenReturn(new DataContract("ORDER_CREATED", 1, fields,
                        List.of(new QualityRule("amount", "GREATER_THAN", "0"))));

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("amount", 49.99);
        ValidationResult result = validationService.validate(
                new EventEnvelope("ORDER_CREATED", 1, "id-4", payload));

        assertThat(result.isValid()).isTrue();
    }
}