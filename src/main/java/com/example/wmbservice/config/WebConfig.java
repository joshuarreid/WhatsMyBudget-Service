package com.example.wmbservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global CORS configuration using origins from application.properties/environment variable.
 * Logs method entry, resolved origins, and configuration.
 */
@Configuration
public class WebConfig {
    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    /**
     * Spring Security uses this CORS config when http.cors() is enabled.
     * Using allowedOriginPatterns keeps credentials support while allowing env-configured origins.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.info("WebConfig.corsConfigurationSource() entry - Raw origins string: {}", corsAllowedOrigins);

        List<String> allowedOriginsList = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        CorsConfiguration cfg = new CorsConfiguration();
        // With allowCredentials(true), prefer patterns to avoid exact-string mismatch issues in prod.
        cfg.setAllowedOriginPatterns(allowedOriginsList);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("content-type", "x-transaction-id", "authorization"));
        cfg.setExposedHeaders(List.of("X-Transaction-ID"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);

        logger.info("CORS configuration applied for /** to origin patterns: {}", allowedOriginsList);
        return source;
    }
}