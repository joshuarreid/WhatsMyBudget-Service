package com.example.wmbservice.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetLimitRequestValidationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesAndDeserializesNullableLimits() throws Exception {
        BudgetLimitRequest request = new BudgetLimitRequest();
        request.setTotalLimit(new BigDecimal("125.50"));

        String json = objectMapper.writeValueAsString(request);
        BudgetLimitRequest roundTrip = objectMapper.readValue(json, BudgetLimitRequest.class);

        assertThat(roundTrip.getEssentialLimit()).isNull();
        assertThat(roundTrip.getNonessentialLimit()).isNull();
        assertThat(roundTrip.getTotalLimit()).isEqualByComparingTo("125.50");
    }

    @Test
    void supportsAllThreeLimitFields() {
        BudgetLimitRequest request = new BudgetLimitRequest();
        request.setEssentialLimit(new BigDecimal("10.00"));
        request.setNonessentialLimit(new BigDecimal("15.00"));
        request.setTotalLimit(new BigDecimal("25.00"));

        assertThat(request.getEssentialLimit()).isEqualByComparingTo("10.00");
        assertThat(request.getNonessentialLimit()).isEqualByComparingTo("15.00");
        assertThat(request.getTotalLimit()).isEqualByComparingTo("25.00");
    }
}
