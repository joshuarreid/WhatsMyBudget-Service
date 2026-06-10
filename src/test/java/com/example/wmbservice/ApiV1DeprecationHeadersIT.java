package com.example.wmbservice;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "api.v1.mode=deprecated"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiV1DeprecationHeadersIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MeterRegistry meterRegistry;

    @Test
    void v1ErrorResponseIncludesDeprecationHeaders() throws Exception {
        mockMvc.perform(get("/api/payment-summary")
                        .param("accounts", "josh")
                        .param("statementPeriod", " "))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Sunset", "Wed, 31 Dec 2026 23:59:59 GMT"))
                .andExpect(header().string("Link", "</api/v2>; rel=\"successor-version\""));
    }

    @Test
    void v2ResponseDoesNotIncludeDeprecationHeaders() throws Exception {
        mockMvc.perform(get("/api/v2/payment-summary")
                        .param("accounts", "josh")
                        .param("statementPeriod", "MAY2026"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"))
                .andExpect(header().doesNotExist("Link"));
    }

    @Test
    void v1RequestIncrementsDeprecationTelemetryCounter() throws Exception {
        Counter counter = meterRegistry.find("api.v1.deprecation.hits").counter();
        double before = counter == null ? 0.0 : counter.count();

        mockMvc.perform(get("/api/payment-summary")
                        .param("accounts", "josh")
                        .param("statementPeriod", " "))
                .andExpect(status().isBadRequest());

        Counter afterCounter = meterRegistry.find("api.v1.deprecation.hits").counter();
        assertThat(afterCounter).isNotNull();
        assertThat(afterCounter.count()).isEqualTo(before + 1.0d);
    }
}
