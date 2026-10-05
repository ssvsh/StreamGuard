package com.streamguard.contract;

import com.streamguard.common.model.FieldDefinition;
import com.streamguard.common.model.FieldType;
import com.streamguard.contract.api.dto.CreateContractRequest;
import com.streamguard.contract.service.ContractService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class ContractServiceIntegrationIT {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:8.0");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired ContractService service;

    @Test
    void persistsAndRetrievesContract() {
        var request = new CreateContractRequest(
                "TEST_EVENT", 1,
                Map.of("id", new FieldDefinition(FieldType.STRING, true)),
                List.of());

        var saved = service.create(request);

        assertThat(saved.getId()).isNotNull();
        assertThat(service.get("TEST_EVENT", 1).getFields()).containsKey("id");
    }
}