package com.streamguard.common.model;

public record QualityRule(String field, String operator, String expectedValue) {
}