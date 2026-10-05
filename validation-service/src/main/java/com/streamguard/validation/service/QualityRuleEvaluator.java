package com.streamguard.validation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.streamguard.common.model.QualityRule;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class QualityRuleEvaluator {

    public boolean evaluate(JsonNode value, QualityRule rule) {
        if (value == null || value.isNull()) {
            return "NULL".equalsIgnoreCase(rule.operator());
        }
        return switch (rule.operator().toUpperCase()) {
            case "GREATER_THAN"     -> value.asDouble() > Double.parseDouble(rule.expectedValue());
            case "LESS_THAN"        -> value.asDouble() < Double.parseDouble(rule.expectedValue());
            case "EQUALS"           -> value.asText().equals(rule.expectedValue());
            case "NOT_EMPTY"        -> !value.asText().isBlank();
            case "IN"               -> Set.of(rule.expectedValue().split(",")).contains(value.asText());
            case "MATCHES_REGEX"    -> value.asText().matches(rule.expectedValue());
            default                 -> true;
        };
    }
}