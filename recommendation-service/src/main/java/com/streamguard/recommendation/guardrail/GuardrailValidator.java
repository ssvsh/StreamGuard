package com.streamguard.recommendation.guardrail;

import com.streamguard.common.model.Compatibility;
import com.streamguard.common.model.MigrationRecommendation;
import com.streamguard.common.model.SchemaAnalysis;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GuardrailValidator {

    public MigrationRecommendation enforce(SchemaAnalysis analysis, MigrationRecommendation llm) {
        String enforcedSeverity = reconcileSeverity(analysis.compatibility(), llm.severity());

        List<String> risks = new ArrayList<>(llm.risks());
        if (!enforcedSeverity.equalsIgnoreCase(llm.severity())) {
            risks.add("Severity escalated by guardrail to match deterministic verdict (" + analysis.compatibility() + ").");
        }

        return new MigrationRecommendation(
                enforcedSeverity,
                llm.summary(),
                sanitize(llm.migrationStrategy()),
                sanitize(llm.rollbackStrategy()),
                risks);
    }

    private String reconcileSeverity(Compatibility compat, String llmSeverity) {
        String s = llmSeverity == null ? "" : llmSeverity.toUpperCase();
        return switch (compat) {
            case BREAKING -> (s.equals("HIGH") || s.equals("CRITICAL")) ? s : "HIGH";
            case POTENTIALLY_BREAKING -> (s.equals("LOW")) ? "MEDIUM" : (s.isBlank() ? "MEDIUM" : s);
            case COMPATIBLE -> (s.isBlank() ? "LOW" : s);
        };
    }

    private List<String> sanitize(List<String> input) {
        if (input == null) return List.of();
        return input.stream()
                .filter(s -> s != null && !s.isBlank())
                .limit(10)
                .toList();
    }
}