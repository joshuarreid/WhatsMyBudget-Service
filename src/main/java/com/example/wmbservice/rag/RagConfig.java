package com.example.wmbservice.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RagConfig {

    @Bean
    public RestClient ragRestClient() {
        return RestClient.builder().build();
    }

    @Bean
    public RagDocumentClient ragDocumentClient(
            RestClient ragRestClient,
            @Value("${rag.base-url:http://localhost:8080}") String ragBaseUrl
    ) {
        return new RagDocumentClient(ragRestClient, ragBaseUrl);
    }
}

