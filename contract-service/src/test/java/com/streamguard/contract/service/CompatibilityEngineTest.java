package com.streamguard.contract.service;

import com.streamguard.common.model.ChangeType;
import com.streamguard.common.model.Compatibility;
import com.streamguard.common.model.FieldDefinition;
import com.streamguard.common.model.FieldType;
import com.streamguard.contract.persistence.ContractDocument;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompatibilityEngineTest {

    private final CompatibilityEngine engine = new CompatibilityEngine(new SchemaDiffService());

    @Test
    void addingOptionalFieldIsCompatible() {
        var v1 = contract(1, Map.of("a", new FieldDefinition(FieldType.STRING, true)));
        var fields = new LinkedHashMap<String, FieldDefinition>();
        fields.put("a", new FieldDefinition(FieldType.STRING, true));
        fields.put("b", new FieldDefinition(FieldType.STRING, false));
        var v2 = contract(2, fields);

        var analysis = engine.analyze(v1, v2);

        assertThat(analysis.compatibility()).isEqualTo(Compatibility.COMPATIBLE);
        assertThat(analysis.changes()).hasSize(1);
        assertThat(analysis.changes().get(0).type()).isEqualTo(ChangeType.FIELD_ADDED);
    }

    @Test
    void removingRequiredFieldIsBreaking() {
        var v1 = contract(1, Map.of(
                "a", new FieldDefinition(FieldType.STRING, true),
                "b", new FieldDefinition(FieldType.STRING, true)));
        var v2 = contract(2, Map.of("a", new FieldDefinition(FieldType.STRING, true)));

        var analysis = engine.analyze(v1, v2);

        assertThat(analysis.compatibility()).isEqualTo(Compatibility.BREAKING);
    }

    @Test
    void typeChangeIsBreaking() {
        var v1 = contract(1, Map.of("a", new FieldDefinition(FieldType.STRING, true)));
        var v2 = contract(2, Map.of("a", new FieldDefinition(FieldType.INTEGER, true)));

        var analysis = engine.analyze(v1, v2);

        assertThat(analysis.compatibility()).isEqualTo(Compatibility.BREAKING);
    }

    @Test
    void addingRequiredFieldIsBreaking() {
        var v1 = contract(1, Map.of("a", new FieldDefinition(FieldType.STRING, true)));
        var fields = new LinkedHashMap<String, FieldDefinition>();
        fields.put("a", new FieldDefinition(FieldType.STRING, true));
        fields.put("b", new FieldDefinition(FieldType.STRING, true));
        var v2 = contract(2, fields);

        assertThat(engine.analyze(v1, v2).compatibility()).isEqualTo(Compatibility.BREAKING);
    }

    @Test
    void removingOptionalFieldIsPotentiallyBreaking() {
        var v1 = contract(1, Map.of(
                "a", new FieldDefinition(FieldType.STRING, true),
                "b", new FieldDefinition(FieldType.STRING, false)));
        var v2 = contract(2, Map.of("a", new FieldDefinition(FieldType.STRING, true)));

        assertThat(engine.analyze(v1, v2).compatibility()).isEqualTo(Compatibility.POTENTIALLY_BREAKING);
    }

    private ContractDocument contract(int version, Map<String, FieldDefinition> fields) {
        return ContractDocument.builder()
                .eventType("ORDER_CREATED")
                .version(version)
                .fields(fields)
                .qualityRules(List.of())
                .build();
    }
}