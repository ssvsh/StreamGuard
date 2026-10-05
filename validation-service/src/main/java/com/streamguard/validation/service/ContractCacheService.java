package com.streamguard.validation.service;

import com.streamguard.common.model.DataContract;
import com.streamguard.common.model.FieldDefinition;
import com.streamguard.common.model.QualityRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractCacheService {

    private final RedisTemplate<String, DataContract> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${streamguard.contract-service-url}")
    private String contractServiceUrl;

    @Value("${streamguard.cache-ttl-minutes}")
    private long cacheTtlMinutes;

    public DataContract getContract(String eventType, Integer version) {
        String key = "contract:" + eventType + ":" + version;
        DataContract cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        DataContract contract = fetch(eventType, version);
        redisTemplate.opsForValue().set(key, contract, Duration.ofMinutes(cacheTtlMinutes));
        return contract;
    }

    @SuppressWarnings("unchecked")
    private DataContract fetch(String eventType, Integer version) {
        String url = contractServiceUrl + "/api/contracts/" + eventType + "/" + version;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Empty contract response for " + eventType + " v" + version);
        }
        Map<String, Map<String, Object>> rawFields = (Map<String, Map<String, Object>>) body.get("fields");
        Map<String, FieldDefinition> fields = new java.util.LinkedHashMap<>();
        if (rawFields != null) {
            rawFields.forEach((k, v) -> fields.put(k,
                    new FieldDefinition(
                            com.streamguard.common.model.FieldType.valueOf((String) v.get("type")),
                            Boolean.TRUE.equals(v.get("required")))));
        }
        List<Map<String, String>> rawRules = (List<Map<String, String>>) body.get("qualityRules");
        List<QualityRule> rules = rawRules == null ? List.of() : rawRules.stream()
                .map(r -> new QualityRule(r.get("field"), r.get("operator"), r.get("expectedValue")))
                .toList();
        return new DataContract(eventType, version, fields, rules);
    }
}