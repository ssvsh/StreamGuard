package com.streamguard.recommendation.api;

import com.streamguard.common.model.MigrationRecommendation;
import com.streamguard.common.model.SchemaAnalysis;
import com.streamguard.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService service;

    @PostMapping
    public Mono<MigrationRecommendation> recommend(@RequestBody SchemaAnalysis analysis) {
        return service.recommend(analysis);
    }
}