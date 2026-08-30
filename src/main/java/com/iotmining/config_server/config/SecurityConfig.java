package com.iotmining.config_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * This service had no SecurityConfig - no security dependency at all -
 * before this pass. Every endpoint Spring Cloud Config Server exposes
 * (/{application}/{profile}, /{application}/{profile}/{label}, /{name}-
 * {profile}.yml, ...) served whatever the backing git repo contains for
 * any application/profile name a caller supplied, to anyone who could
 * reach the port, no credentials required.
 *
 * Basic Auth required on everything except /actuator/health (needed
 * unauthenticated for an external health check/orchestrator) and /error.
 * Same reasoning as eureka-server's own SecurityConfig this pass - a
 * dedicated credential, not shared with anything else.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
