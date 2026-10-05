package com.streamguard.contract.api;

import com.streamguard.common.model.SchemaAnalysis;
import com.streamguard.contract.api.dto.CreateContractRequest;
import com.streamguard.contract.persistence.ContractDocument;
import com.streamguard.contract.service.CompatibilityEngine;
import com.streamguard.contract.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final CompatibilityEngine compatibilityEngine;

    @PostMapping
    public ResponseEntity<ContractDocument> create(@Valid @RequestBody CreateContractRequest request) {
        return ResponseEntity.ok(contractService.create(request));
    }

    @GetMapping("/{eventType}/{version}")
    public ResponseEntity<ContractDocument> get(@PathVariable String eventType,
                                                @PathVariable Integer version) {
        return ResponseEntity.ok(contractService.get(eventType, version));
    }

    @GetMapping("/{eventType}/latest")
    public ResponseEntity<ContractDocument> latest(@PathVariable String eventType) {
        return ResponseEntity.ok(contractService.getLatest(eventType));
    }

    @GetMapping("/{eventType}")
    public ResponseEntity<List<ContractDocument>> list(@PathVariable String eventType) {
        return ResponseEntity.ok(contractService.list(eventType));
    }

    @GetMapping
    public ResponseEntity<List<ContractDocument>> all() {
        return ResponseEntity.ok(contractService.all());
    }

    @GetMapping("/{eventType}/compare")
    public ResponseEntity<SchemaAnalysis> compare(@PathVariable String eventType,
                                                  @RequestParam("from") Integer from,
                                                  @RequestParam("to") Integer to) {
        ContractDocument oldContract = contractService.get(eventType, from);
        ContractDocument newContract = contractService.get(eventType, to);
        return ResponseEntity.ok(compatibilityEngine.analyze(oldContract, newContract));
    }
}