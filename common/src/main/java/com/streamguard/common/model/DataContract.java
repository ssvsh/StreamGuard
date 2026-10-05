package com.streamguard.common.model;

import java.util.List;
import java.util.Map;

public record DataContract(
        String eventType,
        int version,
        Map<String, FieldDefinition> fields,
        List<QualityRule> qualityRules
) {
}