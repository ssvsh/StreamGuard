package com.streamguard.recommendation.service;

import com.streamguard.common.model.Compatibility;
import com.streamguard.common.model.MigrationRecommendation;
import com.streamguard.common.model.SchemaAnalysis;
import com.streamguard.recommendation.guardrail.GuardrailValidator;
import com.streamguard.recommendation.llm.LlmClient;
import com.streamguard.recommendation.llm.LlmResponseParser;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final LlmResponseParser parser;
    private final GuardrailValidator guardrail;
    private final MeterRegistry meterRegistry;

    private Counter llmCalls;
    private Counter llmFailures;

    @PostConstruct
    void init() {
        llmCalls    = Counter.builder("streamguard.llm.calls").register(meterRegistry);
        llmFailures = Counter.builder("streamguard.llm.failures").register(meterRegistry);
    }

    @CircuitBreaker(name = "llm", fallbackMethod = "fallback")
    @Retry(name = "llm")
    public Mono<MigrationRecommendation> recommend(SchemaAnalysis analysis) {
        llmCalls.increment();
        String prompt = promptBuilder.build(analysis);
        return llmClient.generate(prompt)
                .map(parser::parse)
                .map(rec -> guardrail.enforce(analysis, rec));
    }

    @SuppressWarnings("unused")
    private Mono<MigrationRecommendation> fallback(SchemaAnalysis analysis, Throwable ex) {
        log.warn("LLM unavailable; returning deterministic fallback: {}", ex.toString());
        llmFailures.increment();
        String severity = switch (analysis.compatibility()) {
            case BREAKING -> "HIGH";
            case POTENTIALLY_BREAKING -> "MEDIUM";
            case COMPATIBLE -> "LOW";
        };
        return Mono.just(new MigrationRecommendation(
                severity,
                "LLM unavailable. Deterministic verdict: " + analysis.compatibility(),
                defaultMigration(analysis.compatibility()),
                List.of("Revert producer to previous schema", "Pause consumers until compatible contracts are deployed"),
                List.of("No AI commentary available", "Rely on deterministic compatibility rules only")));
    }

    private List<String> defaultMigration(Compatibility compat) {
        return switch (compat) {
            case BREAKING -> List.of(
                    "Introduce new fields alongside old fields",
                    "Update consumers to read both schemas",
                    "Backfill historical events",
                    "Remove deprecated fields only after all consumers migrate");
            case POTENTIALLY_BREAKING -> List.of(
                    "Confirm no consumer depends on removed optional fields",
                    "Deploy change behind a feature flag",
                    "Monitor consumer error rates for one release cycle");
            case COMPATIBLE -> List.of("Deploy producer change", "Monitor consumer error rates");
        };
    }
}