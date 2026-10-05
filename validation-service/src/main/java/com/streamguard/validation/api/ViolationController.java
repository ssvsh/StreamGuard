package com.streamguard.validation.api;

import com.streamguard.validation.persistence.ViolationDocument;
import com.streamguard.validation.persistence.ViolationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
public class ViolationController {

    private final ViolationRepository repository;

    @GetMapping
    public ResponseEntity<List<ViolationDocument>> list(
            @RequestParam String eventType,
            @RequestParam(defaultValue = "100") int limit) {
        var page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(repository.findByEventType(eventType, page));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> summary(@RequestParam String eventType) {
        return ResponseEntity.ok(Map.of(
                "total", repository.countByEventType(eventType),
                "missing_field", repository.countByEventTypeAndViolationType(eventType, "MISSING_FIELD"),
                "type_mismatch", repository.countByEventTypeAndViolationType(eventType, "TYPE_MISMATCH"),
                "rule_failure", repository.countByEventTypeAndViolationType(eventType, "RULE_FAILURE")));
    }
}