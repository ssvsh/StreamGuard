package com.streamguard.contract.api.dto;

import com.streamguard.common.model.FieldDefinition;
import com.streamguard.common.model.QualityRule;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record CreateContractRequest(
        @NotBlank String eventType,
        @NotNull @Min(1) Integer version,
        @NotNull Map<String, FieldDefinition> fields,
        List<QualityRule> qualityRules
) {
}