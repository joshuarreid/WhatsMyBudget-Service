package com.example.wmbservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // Ensure CORS is handled inside Spring Security (so preflights aren't blocked)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Allow all CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public login + setup endpoints
                        .requestMatchers("/auth/login", "/auth/hash").permitAll()

                        // v2 requires a valid backend-minted JWT
                        .requestMatchers("/api/v2/**").authenticated()

                        // v1 stays open
                        .requestMatchers("/api/**").permitAll()

                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
