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
        "api.v1.mode=active"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiV1ActiveModeIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MeterRegistry meterRegistry;

    @Test
    void v1ResponseDoesNotIncludeDeprecationHeadersOrIncrementCounter() throws Exception {
        Counter counter = meterRegistry.find("api.v1.deprecation.hits").counter();
        double before = counter == null ? 0.0 : counter.count();

        mockMvc.perform(get("/api/payment-summary")
                        .param("accounts", "josh")
                        .param("statementPeriod", " "))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"))
                .andExpect(header().doesNotExist("Link"));

        Counter afterCounter = meterRegistry.find("api.v1.deprecation.hits").counter();
        double after = afterCounter == null ? 0.0 : afterCounter.count();
        assertThat(after).isEqualTo(before);
    }
}

