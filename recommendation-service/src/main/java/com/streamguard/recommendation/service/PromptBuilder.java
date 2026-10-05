package com.streamguard.recommendation.service;

import com.streamguard.common.model.SchemaAnalysis;
import com.streamguard.common.model.SchemaChange;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class PromptBuilder {

    public String build(SchemaAnalysis analysis) {
        String changeList = analysis.changes().stream()
                .map(this::formatChange)
                .collect(Collectors.joining("\n"));

        return """
            You are a senior distributed-systems engineer reviewing a Kafka schema change.

            Deterministic compatibility verdict (authoritative):
            %s

            Event type: %s
            Old version: %d
            New version: %d

            Changes:
            %s

            Respond with a single JSON object only, no prose, matching exactly this shape:

            {
              "severity": "LOW" | "MEDIUM" | "HIGH" | "CRITICAL",
              "summary": "one-sentence summary of the problem",
              "migrationStrategy": ["step 1", "step 2", ...],
              "rollbackStrategy": ["step 1", "step 2", ...],
              "risks": ["risk 1", "risk 2", ...]
            }

            Rules:
            - Do not invent infrastructure details that are not implied by the changes.
            - Do not contradict the deterministic verdict.
            - Keep each step actionable and concrete.
            """.formatted(
                    analysis.compatibility(),
                    analysis.eventType(),
                    analysis.oldVersion(),
                    analysis.newVersion(),
                    changeList);
    }

    private String formatChange(SchemaChange c) {
        return "- field=%s change=%s oldType=%s newType=%s oldRequired=%s newRequired=%s".formatted(
                c.field(), c.type(),
                c.oldType(), c.newType(),
                c.oldRequired(), c.newRequired());
    }
}