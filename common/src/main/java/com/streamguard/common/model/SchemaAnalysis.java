package com.streamguard.common.model;

import java.util.List;

public record SchemaAnalysis(
        String eventType,
        int oldVersion,
        int newVersion,
        Compatibility compatibility,
        List<SchemaChange> changes
) {
}