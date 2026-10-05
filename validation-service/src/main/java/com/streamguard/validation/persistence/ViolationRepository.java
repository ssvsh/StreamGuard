package com.streamguard.validation.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ViolationRepository extends MongoRepository<ViolationDocument, String> {
    List<ViolationDocument> findByEventType(String eventType, Pageable pageable);
    long countByEventType(String eventType);
    long countByEventTypeAndViolationType(String eventType, String violationType);
}