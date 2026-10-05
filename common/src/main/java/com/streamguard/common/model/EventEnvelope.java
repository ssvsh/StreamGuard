package com.streamguard.common.model;

import com.fasterxml.jackson.databind.JsonNode;

public record EventEnvelope(
        String eventType,
        Integer schemaVersion,
        String eventId,
        JsonNode payload
) {
}