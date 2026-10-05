package com.streamguard.recommendation.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "streamguard.llm", name = "provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiLlmClient implements LlmClient {

    @Value("${streamguard.gemini.base-url}")
    private String baseUrl;

    @Value("${streamguard.gemini.model}")
    private String model;

    @Value("${streamguard.gemini.api-key}")
    private String apiKey;

    @Override
    public Mono<String> generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return Mono.error(new IllegalStateException("GEMINI_API_KEY not configured"));
        }
        WebClient client = WebClient.create(baseUrl);
        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))));

        return client.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractText);
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> response) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return "";
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) return "";
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) return "";
        return String.valueOf(parts.get(0).get("text"));
    }
}