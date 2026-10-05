package com.streamguard.validation.api;

import com.streamguard.validation.persistence.ViolationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
public class QualityScoreController {

    private final ViolationRepository violationRepository;

    @GetMapping("/{eventType}")
    public ResponseEntity<Map<String, Object>> score(@PathVariable String eventType,
                                                     @RequestParam(defaultValue = "0") long processedTotal) {
        long violations = violationRepository.countByEventType(eventType);
        long effectiveTotal = Math.max(processedTotal, violations);
        double score = effectiveTotal == 0 ? 100.0
                : Math.max(0.0, 100.0 * (effectiveTotal - violations) / effectiveTotal);
        return ResponseEntity.ok(Map.of(
                "eventType", eventType,
                "totalEvents", effectiveTotal,
                "violations", violations,
                "qualityScore", Math.round(score * 100.0) / 100.0));
    }
}