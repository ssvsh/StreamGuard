package com.streamguard.contract.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends MongoRepository<ContractDocument, String> {

    Optional<ContractDocument> findByEventTypeAndVersion(String eventType, Integer version);

    Optional<ContractDocument> findTopByEventTypeOrderByVersionDesc(String eventType);

    List<ContractDocument> findByEventTypeOrderByVersionAsc(String eventType);
}