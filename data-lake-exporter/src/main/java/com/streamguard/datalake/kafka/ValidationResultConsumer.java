package com.streamguard.datalake.kafka;

import com.streamguard.common.kafka.Topics;
import com.streamguard.common.model.ValidationResult;
import com.streamguard.datalake.service.S3ParquetWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(S3ParquetWriter.class)
public class ValidationResultConsumer {

    private final S3ParquetWriter writer;

    @KafkaListener(topics = Topics.VALIDATION_RESULTS, groupId = "streamguard-data-lake")
    public void consume(ValidationResult result) {
        writer.append(result);
    }

    @Scheduled(fixedRateString = "${streamguard.export.flush-interval-ms}")
    public void flush() {
        writer.flush();
    }
}