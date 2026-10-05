package com.streamguard.recommendation.llm;

import reactor.core.publisher.Mono;

public interface LlmClient {
    Mono<String> generate(String prompt);
}