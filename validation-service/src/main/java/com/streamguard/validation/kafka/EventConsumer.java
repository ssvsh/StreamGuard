package com.streamguard.validation.kafka;

import com.streamguard.common.kafka.Topics;
import com.streamguard.common.model.EventEnvelope;
import com.streamguard.common.model.ValidationResult;
import com.streamguard.validation.metrics.ValidationMetrics;
import com.streamguard.validation.service.IdempotencyService;
import com.streamguard.validation.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final ValidationService validationService;
    private final IdempotencyService idempotency;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ValidationMetrics metrics;

    @KafkaListener(
            topics = Topics.ORDER_EVENTS,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(EventEnvelope event) {
        try {
            if (idempotency.seenOrMark(event.eventId())) {
                metrics.recordDuplicateSkipped();
                return;
            }

            ValidationResult result = validationService.validate(event);
            kafkaTemplate.send(Topics.VALIDATION_RESULTS, event.eventType(), result);

            if (!result.isValid()) {
                kafkaTemplate.send(Topics.ORDER_EVENTS_DLQ, event.eventType(), event);
                metrics.recordDlqWrite();
            }
        } catch (RuntimeException ex) {
            log.error("Validation pipeline error for event {}", event.eventId(), ex);
            kafkaTemplate.send(Topics.ORDER_EVENTS_RETRY_1, event.eventType(), event);
        }
    }

    @KafkaListener(
            topics = Topics.ORDER_EVENTS_RETRY_1,
            groupId = "${spring.kafka.consumer.group-id}-retry-1"
    )
    public void retry1(EventEnvelope event) {
        try {
            validationService.validate(event);
        } catch (RuntimeException ex) {
            kafkaTemplate.send(Topics.ORDER_EVENTS_RETRY_2, event.eventType(), event);
        }
    }

    @KafkaListener(
            topics = Topics.ORDER_EVENTS_RETRY_2,
            groupId = "${spring.kafka.consumer.group-id}-retry-2"
    )
    public void retry2(EventEnvelope event) {
        try {
            validationService.validate(event);
        } catch (RuntimeException ex) {
            kafkaTemplate.send(Topics.ORDER_EVENTS_DLQ, event.eventType(), event);
            metrics.recordDlqWrite();
        }
    }
}