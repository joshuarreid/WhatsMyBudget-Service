package com.example.wmbservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
     * Configures CORS to allow requests from frontend(s).
     * Origins are loaded from application.properties/environment variable.
     * @return WebMvcConfigurer instance
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        logger.info("WebConfig.corsConfigurer() entry - Raw origins string: {}", corsAllowedOrigins);

        List<String> allowedOriginsList = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        logger.info("Resolved allowedOriginsList: {}", allowedOriginsList);

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Apply CORS to all endpoints (incl. /auth/login) so the browser can complete preflight.
                registry.addMapping("/**")
                        .allowedOrigins(allowedOriginsList.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("content-type", "x-transaction-id", "authorization")
                        .exposedHeaders("X-Transaction-ID")
                        .allowCredentials(true);

                logger.info("CORS mapping applied for /** to origins: {}", allowedOriginsList);
            }
        };
    }
}