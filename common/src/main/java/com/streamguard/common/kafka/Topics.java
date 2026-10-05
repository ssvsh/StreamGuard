package com.streamguard.common.kafka;

public final class Topics {

    public static final String ORDER_EVENTS            = "order-events";
    public static final String VALIDATION_RESULTS      = "validation-results";
    public static final String ORDER_EVENTS_DLQ        = "order-events-dlq";
    public static final String ORDER_EVENTS_RETRY_1    = "order-events-retry-1";
    public static final String ORDER_EVENTS_RETRY_2    = "order-events-retry-2";
    public static final String SCHEMA_DRIFT_EVENTS     = "schema-drift-events";

    private Topics() {}
}