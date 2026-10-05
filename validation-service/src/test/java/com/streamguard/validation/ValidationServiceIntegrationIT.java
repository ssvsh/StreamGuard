package com.streamguard.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.streamguard.common.kafka.Topics;
import com.streamguard.common.model.EventEnvelope;
import com.streamguard.validation.persistence.ViolationDocument;
import com.streamguard.validation.persistence.ViolationRepository;
import com.streamguard.validation.service.ContractCacheService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ValidationServiceIntegrationIT {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("spring.data.redis.host", redis::getHost);
        r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        r.add("streamguard.contract-service-url", () -> "http://localhost:0");
    }

    @MockBean
    ContractCacheService contractCacheService;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    ViolationRepository violationRepository;

    @Autowired
    ObjectMapper mapper;

    @Test
    void invalidEventProducesViolation() {
        LinkedHashMap<String, com.streamguard.common.model.FieldDefinition> fields = new LinkedHashMap<>();
        fields.put("amount", new com.streamguard.common.model.FieldDefinition(
                com.streamguard.common.model.FieldType.DECIMAL, true));
        when(contractCacheService.getContract("ORDER_CREATED", 1)).thenReturn(
                new com.streamguard.common.model.DataContract("ORDER_CREATED", 1, fields, List.of()));

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        String eventId = UUID.randomUUID().toString();
        EventEnvelope event = new EventEnvelope("ORDER_CREATED", 1, eventId, payload);

        kafkaTemplate.send(Topics.ORDER_EVENTS, event);

        Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    List<ViolationDocument> all = violationRepository.findAll();
                    assertThat(all).anyMatch(v -> eventId.equals(v.getEventId())
                            && "MISSING_FIELD".equals(v.getViolationType()));
                });
    }
}