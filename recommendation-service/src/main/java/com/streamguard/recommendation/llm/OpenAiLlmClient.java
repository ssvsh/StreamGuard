package com.streamguard.recommendation.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "streamguard.llm", name = "provider", havingValue = "openai")
public class OpenAiLlmClient implements LlmClient {

    @Value("${streamguard.openai.base-url}")
    private String baseUrl;

    @Value("${streamguard.openai.model}")
    private String model;

    @Value("${streamguard.openai.api-key}")
    private String apiKey;

    @Override
    public Mono<String> generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return Mono.error(new IllegalStateException("OPENAI_API_KEY not configured"));
        }
        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "response_format", Map.of("type", "json_object"));

        return client.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractText);
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return "";
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message == null ? "" : String.valueOf(message.get("content"));
    }
}