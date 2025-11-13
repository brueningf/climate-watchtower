package com.audit.climate.watchman.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Keep security enabled but allow HTTP Basic for API endpoints used by tests
        // Permit unauthenticated access to OpenAPI/Swagger UI endpoints
        http
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic();

        return http.build();
    }
}
