package com.streamguard.validation.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidationMetrics {

    private final MeterRegistry registry;

    private Counter eventsProcessed;
    private Counter validationFailures;
    private Counter schemaDrifts;
    private Counter dlqWrites;
    private Counter duplicatesSkipped;
    private Timer validationLatency;

    @PostConstruct
    void init() {
        eventsProcessed    = Counter.builder("streamguard.events.processed").register(registry);
        validationFailures = Counter.builder("streamguard.validation.failures").register(registry);
        schemaDrifts       = Counter.builder("streamguard.schema.drifts").register(registry);
        dlqWrites          = Counter.builder("streamguard.dlq.writes").register(registry);
        duplicatesSkipped  = Counter.builder("streamguard.duplicates.skipped").register(registry);
        validationLatency  = Timer.builder("streamguard.validation.latency")
                .publishPercentileHistogram()
                .register(registry);
    }

    public Timer.Sample startValidation() { return Timer.start(registry); }
    public void stopValidation(Timer.Sample s) { s.stop(validationLatency); }
    public void recordEventProcessed()      { eventsProcessed.increment(); }
    public void recordValidationFailure()   { validationFailures.increment(); }
    public void recordSchemaDrift()         { schemaDrifts.increment(); }
    public void recordDlqWrite()            { dlqWrites.increment(); }
    public void recordDuplicateSkipped()    { duplicatesSkipped.increment(); }
}