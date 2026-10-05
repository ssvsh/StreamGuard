package com.streamguard.validation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamguard.common.model.*;
import com.streamguard.validation.metrics.ValidationMetrics;
import com.streamguard.validation.persistence.ViolationDocument;
import com.streamguard.validation.persistence.ViolationRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final ContractCacheService contractCache;
    private final QualityRuleEvaluator ruleEvaluator;
    private final ViolationRepository violationRepository;
    private final ValidationMetrics metrics;

    public ValidationResult validate(EventEnvelope event) {
        Timer.Sample sample = metrics.startValidation();
        try {
            DataContract contract = contractCache.getContract(event.eventType(), event.schemaVersion());

            List<Violation> violations = new ArrayList<>();
            contract.fields().forEach((name, def) -> {
                JsonNode value = event.payload().get(name);
                if (value == null || value.isNull()) {
                    if (def.required()) {
                        violations.add(Violation.missing(name));
                    }
                    return;
                }
                if (!matchesType(value, def.type())) {
                    violations.add(Violation.typeMismatch(name, def.type()));
                }
            });

            if (contract.qualityRules() != null) {
                for (QualityRule rule : contract.qualityRules()) {
                    JsonNode value = event.payload().get(rule.field());
                    if (!ruleEvaluator.evaluate(value, rule)) {
                        violations.add(Violation.ruleFailure(
                                rule.field(),
                                rule.operator() + " " + rule.expectedValue()));
                    }
                }
            }

            ValidationResult result = new ValidationResult(
                    event.eventType(),
                    event.schemaVersion(),
                    event.eventId(),
                    violations,
                    Instant.now());

            metrics.recordEventProcessed();
            if (!result.isValid()) {
                metrics.recordValidationFailure();
                persistViolations(event, violations);
            }
            return result;
        } finally {
            metrics.stopValidation(sample);
        }
    }

    private void persistViolations(EventEnvelope event, List<Violation> violations) {
        List<ViolationDocument> docs = violations.stream()
                .map(v -> ViolationDocument.builder()
                        .eventType(event.eventType())
                        .schemaVersion(event.schemaVersion())
                        .eventId(event.eventId())
                        .violationType(v.violationType())
                        .field(v.field())
                        .message(v.message())
                        .timestamp(Instant.now())
                        .build())
                .toList();
        violationRepository.saveAll(docs);
    }

    private boolean matchesType(JsonNode node, FieldType type) {
        return switch (type) {
            case STRING  -> node.isTextual();
            case INTEGER -> node.isInt();
            case LONG    -> node.isLong() || node.isInt();
            case DOUBLE, DECIMAL -> node.isNumber();
            case BOOLEAN -> node.isBoolean();
        };
    }
}