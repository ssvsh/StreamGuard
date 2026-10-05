package com.streamguard.validation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamguard.common.model.DataContract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, DataContract> contractRedisTemplate(RedisConnectionFactory cf, ObjectMapper mapper) {
        RedisTemplate<String, DataContract> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        template.setKeySerializer(new StringRedisSerializer());
        Jackson2JsonRedisSerializer<DataContract> ser = new Jackson2JsonRedisSerializer<>(mapper, DataContract.class);
        template.setValueSerializer(ser);
        return template;
    }

    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}