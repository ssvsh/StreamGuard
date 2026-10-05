package com.streamguard.datalake.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamguard.common.model.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "streamguard.export", name = "enabled", havingValue = "true")
public class S3ParquetWriter {

    private final ObjectMapper mapper;

    @Value("${streamguard.export.bucket}")
    private String bucket;

    @Value("${streamguard.export.region}")
    private String region;

    @Value("${streamguard.export.batch-size}")
    private int batchSize;

    private S3Client s3;
    private final List<ValidationResult> buffer = new ArrayList<>();

    @PostConstruct
    void init() {
        s3 = S3Client.builder().region(Region.of(region)).build();
    }

    @PreDestroy
    void shutdown() {
        if (!buffer.isEmpty()) flush();
        if (s3 != null) s3.close();
    }

    public synchronized void append(ValidationResult result) {
        buffer.add(result);
        if (buffer.size() >= batchSize) flush();
    }

    public synchronized void flush() {
        if (buffer.isEmpty()) return;
        try {
            byte[] body = mapper.writeValueAsBytes(List.copyOf(buffer));
            LocalDate today = LocalDate.now();
            String key = "validation-results/year=%d/month=%02d/day=%02d/%s.json"
                    .formatted(today.getYear(), today.getMonthValue(), today.getDayOfMonth(), UUID.randomUUID());
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket).key(key).contentType("application/json").build(),
                    RequestBody.fromBytes(body));
            log.info("Flushed {} validation results to s3://{}/{}", buffer.size(), bucket, key);
            buffer.clear();
        } catch (Exception ex) {
            log.error("Failed to flush buffer to S3", ex);
        }
    }
}