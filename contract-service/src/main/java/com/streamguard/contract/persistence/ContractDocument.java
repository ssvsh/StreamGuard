package com.streamguard.contract.persistence;

import com.streamguard.common.model.FieldDefinition;
import com.streamguard.common.model.QualityRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "contracts")
@CompoundIndex(name = "eventType_version_idx", def = "{'eventType': 1, 'version': 1}", unique = true)
public class ContractDocument {

    @Id
    private String id;

    private String eventType;
    private Integer version;
    private Map<String, FieldDefinition> fields;
    private List<QualityRule> qualityRules;
    private Instant createdAt;
}