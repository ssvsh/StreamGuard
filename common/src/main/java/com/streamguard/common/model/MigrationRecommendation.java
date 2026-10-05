package com.streamguard.common.model;

import java.util.List;

public record MigrationRecommendation(
        String severity,
        String summary,
        List<String> migrationStrategy,
        List<String> rollbackStrategy,
        List<String> risks
) {
}