package com.streamguard.validation.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "violations")
public class ViolationDocument {

    @Id
    private String id;

    @Indexed
    private String eventType;
    private Integer schemaVersion;
    @Indexed
    private String eventId;
    @Indexed
    private String violationType;
    private String field;
    private String message;
    @Indexed
    private Instant timestamp;
}