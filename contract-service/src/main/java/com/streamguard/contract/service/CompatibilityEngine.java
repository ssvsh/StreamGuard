package com.streamguard.contract.service;

import com.streamguard.common.model.ChangeType;
import com.streamguard.common.model.Compatibility;
import com.streamguard.common.model.SchemaAnalysis;
import com.streamguard.common.model.SchemaChange;
import com.streamguard.contract.persistence.ContractDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompatibilityEngine {

    private final SchemaDiffService diffService;

    public SchemaAnalysis analyze(ContractDocument oldContract, ContractDocument newContract) {
        List<SchemaChange> changes = diffService.diff(oldContract, newContract);
        return new SchemaAnalysis(
                oldContract.getEventType(),
                oldContract.getVersion(),
                newContract.getVersion(),
                classify(changes),
                changes);
    }

    Compatibility classify(List<SchemaChange> changes) {
        Compatibility worst = Compatibility.COMPATIBLE;
        for (SchemaChange change : changes) {
            Compatibility c = classifyOne(change);
            if (c.ordinal() > worst.ordinal()) {
                worst = c;
            }
        }
        return worst;
    }

    private Compatibility classifyOne(SchemaChange change) {
        return switch (change.type()) {
            case FIELD_ADDED -> Boolean.TRUE.equals(change.newRequired())
                    ? Compatibility.BREAKING
                    : Compatibility.COMPATIBLE;
            case FIELD_REMOVED -> Boolean.TRUE.equals(change.oldRequired())
                    ? Compatibility.BREAKING
                    : Compatibility.POTENTIALLY_BREAKING;
            case TYPE_CHANGED -> Compatibility.BREAKING;
            case REQUIRED_CHANGED -> {
                if (Boolean.FALSE.equals(change.oldRequired()) && Boolean.TRUE.equals(change.newRequired())) {
                    yield Compatibility.BREAKING;
                }
                yield Compatibility.COMPATIBLE;
            }
        };
    }
}