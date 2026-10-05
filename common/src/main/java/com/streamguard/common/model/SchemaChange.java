package com.streamguard.common.model;

public record SchemaChange(
        String field,
        ChangeType type,
        String oldType,
        String newType,
        Boolean oldRequired,
        Boolean newRequired
) {
}