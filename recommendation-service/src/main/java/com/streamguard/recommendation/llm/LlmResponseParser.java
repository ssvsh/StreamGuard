package com.streamguard.recommendation.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamguard.common.model.MigrationRecommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LlmResponseParser {

    private final ObjectMapper mapper;

    public MigrationRecommendation parse(String raw) {
        try {
            String json = extractJson(raw);
            JsonNode node = mapper.readTree(json);
            return new MigrationRecommendation(
                    text(node, "severity"),
                    text(node, "summary"),
                    stringArray(node, "migrationStrategy"),
                    stringArray(node, "rollbackStrategy"),
                    stringArray(node, "risks"));
        } catch (Exception ex) {
            return new MigrationRecommendation(
                    "UNKNOWN",
                    "Could not parse LLM output",
                    List.of(),
                    List.of(),
                    List.of("LLM returned unstructured text"));
        }
    }

    private String extractJson(String raw) {
        if (raw == null) return "{}";
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return "{}";
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText();
    }

    private List<String> stringArray(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || !v.isArray()) return List.of();
        List<String> out = new ArrayList<>();
        v.forEach(e -> out.add(e.asText()));
        return out;
    }
}