package com.streamguard.common.model;

public record Violation(String field, String violationType, String message) {

    public static Violation missing(String field) {
        return new Violation(field, "MISSING_FIELD", "Required field is missing");
    }

    public static Violation typeMismatch(String field, FieldType expected) {
        return new Violation(field, "TYPE_MISMATCH", "Expected " + expected);
    }

    public static Violation ruleFailure(String field, String rule) {
        return new Violation(field, "RULE_FAILURE", rule);
    }
}