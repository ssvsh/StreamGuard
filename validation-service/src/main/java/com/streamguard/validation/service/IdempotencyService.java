package com.streamguard.validation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, String> stringRedisTemplate;

    @Value("${streamguard.idempotency-ttl-hours}")
    private long ttlHours;

    public boolean seenOrMark(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        String key = "processed:event:" + eventId;
        Boolean set = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofHours(ttlHours));
        return !Boolean.TRUE.equals(set);
    }
}