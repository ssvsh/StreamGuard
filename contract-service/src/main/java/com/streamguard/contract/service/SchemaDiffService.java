package com.streamguard.contract.service;

import com.streamguard.common.model.ChangeType;
import com.streamguard.common.model.FieldDefinition;
import com.streamguard.common.model.SchemaChange;
import com.streamguard.contract.persistence.ContractDocument;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SchemaDiffService {

    public List<SchemaChange> diff(ContractDocument oldContract, ContractDocument newContract) {
        List<SchemaChange> changes = new ArrayList<>();
        Map<String, FieldDefinition> oldFields = oldContract.getFields();
        Map<String, FieldDefinition> newFields = newContract.getFields();

        oldFields.forEach((field, oldDef) -> {
            FieldDefinition newDef = newFields.get(field);
            if (newDef == null) {
                changes.add(new SchemaChange(
                        field, ChangeType.FIELD_REMOVED,
                        oldDef.type().name(), null,
                        oldDef.required(), null));
                return;
            }
            if (!Objects.equals(oldDef.type(), newDef.type())) {
                changes.add(new SchemaChange(
                        field, ChangeType.TYPE_CHANGED,
                        oldDef.type().name(), newDef.type().name(),
                        oldDef.required(), newDef.required()));
            }
            if (oldDef.required() != newDef.required()) {
                changes.add(new SchemaChange(
                        field, ChangeType.REQUIRED_CHANGED,
                        oldDef.type().name(), newDef.type().name(),
                        oldDef.required(), newDef.required()));
            }
        });

        newFields.forEach((field, newDef) -> {
            if (!oldFields.containsKey(field)) {
                changes.add(new SchemaChange(
                        field, ChangeType.FIELD_ADDED,
                        null, newDef.type().name(),
                        null, newDef.required()));
            }
        });

        return changes;
    }
}