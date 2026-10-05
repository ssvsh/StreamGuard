package com.streamguard.contract.service;

import com.streamguard.contract.api.dto.CreateContractRequest;
import com.streamguard.contract.persistence.ContractDocument;
import com.streamguard.contract.persistence.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository repository;

    public ContractDocument create(CreateContractRequest request) {
        repository.findByEventTypeAndVersion(request.eventType(), request.version())
                .ifPresent(existing -> {
                    throw new DuplicateKeyException(
                            "Contract " + request.eventType() + " v" + request.version() + " already exists");
                });

        ContractDocument document = ContractDocument.builder()
                .eventType(request.eventType())
                .version(request.version())
                .fields(request.fields())
                .qualityRules(request.qualityRules())
                .createdAt(Instant.now())
                .build();

        return repository.save(document);
    }

    public ContractDocument get(String eventType, Integer version) {
        return repository.findByEventTypeAndVersion(eventType, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contract not found: " + eventType + " v" + version));
    }

    public ContractDocument getLatest(String eventType) {
        return repository.findTopByEventTypeOrderByVersionDesc(eventType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No contract found for " + eventType));
    }

    public List<ContractDocument> list(String eventType) {
        return repository.findByEventTypeOrderByVersionAsc(eventType);
    }

    public List<ContractDocument> all() {
        return repository.findAll();
    }
}