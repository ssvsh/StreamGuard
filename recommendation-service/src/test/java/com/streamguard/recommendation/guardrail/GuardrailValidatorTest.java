package com.streamguard.recommendation.guardrail;

import com.streamguard.common.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuardrailValidatorTest {

    private final GuardrailValidator guardrail = new GuardrailValidator();

    @Test
    void escalatesLowSeverityOnBreakingChange() {
        var analysis = new SchemaAnalysis("ORDER_CREATED", 1, 2, Compatibility.BREAKING, List.of(
                new SchemaChange("customerId", ChangeType.FIELD_REMOVED, "STRING", null, true, null)));
        var llm = new MigrationRecommendation("LOW", "Minor", List.of("a"), List.of("b"), List.of());

        var enforced = guardrail.enforce(analysis, llm);

        assertThat(enforced.severity()).isEqualTo("HIGH");
        assertThat(enforced.risks()).anyMatch(r -> r.contains("escalated"));
    }

    @Test
    void keepsCriticalWhenAlignedWithBreaking() {
        var analysis = new SchemaAnalysis("ORDER_CREATED", 1, 2, Compatibility.BREAKING, List.of());
        var llm = new MigrationRecommendation("CRITICAL", "x", List.of(), List.of(), List.of());

        assertThat(guardrail.enforce(analysis, llm).severity()).isEqualTo("CRITICAL");
    }

    @Test
    void bumpsLowToMediumOnPotentiallyBreaking() {
        var analysis = new SchemaAnalysis("ORDER_CREATED", 1, 2, Compatibility.POTENTIALLY_BREAKING, List.of());
        var llm = new MigrationRecommendation("LOW", "x", List.of(), List.of(), List.of());

        assertThat(guardrail.enforce(analysis, llm).severity()).isEqualTo("MEDIUM");
    }
}