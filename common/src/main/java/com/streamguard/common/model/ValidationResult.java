package com.streamguard.common.model;

import java.time.Instant;
import java.util.List;

public record ValidationResult(
        String eventType,
        Integer schemaVersion,
        String eventId,
        List<Violation> violations,
        Instant timestamp
) {
    public boolean isValid() {
        return violations == null || violations.isEmpty();
    }
}