package com.streamguard.validation.generator;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.streamguard.common.kafka.Topics;
import com.streamguard.common.model.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "streamguard.generator", name = "enabled", havingValue = "true")
public class EventGenerator {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final List<String> CURRENCIES = List.of("USD", "CAD", "EUR", "GBP");

    @Scheduled(fixedRateString = "${streamguard.generator.interval-ms:200}")
    public void tick() {
        kafkaTemplate.send(Topics.ORDER_EVENTS, "ORDER_CREATED", buildEvent());
    }

    private EventEnvelope buildEvent() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        String id = UUID.randomUUID().toString();
        payload.put("eventId", id);
        payload.put("customerId", "C" + ThreadLocalRandom.current().nextInt(1000, 9999));

        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.02) {
            payload.put("amount", "249.99");
        } else if (roll < 0.03) {
            payload.put("amount", -5.00);
        } else {
            payload.put("amount", ThreadLocalRandom.current().nextDouble(10, 1000));
        }

        if (roll >= 0.03 && roll < 0.04) {
            payload.put("currency", "XYZ");
        } else {
            payload.put("currency", CURRENCIES.get(ThreadLocalRandom.current().nextInt(CURRENCIES.size())));
        }

        if (roll >= 0.04 && roll < 0.05) {
            payload.remove("customerId");
        }

        payload.put("status", "CREATED");

        return new EventEnvelope("ORDER_CREATED", 1, id, payload);
    }
}