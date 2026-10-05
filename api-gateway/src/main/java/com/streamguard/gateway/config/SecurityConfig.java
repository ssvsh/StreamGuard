package com.streamguard.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/contracts/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/violations/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/quality/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/contracts/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/recommendations/**").hasRole("ENGINEER")
                        .anyExchange().permitAll())
                .httpBasic(httpBasic -> {})
                .build();
    }
}