package com.example.wmbservice;

import com.example.wmbservice.model.PaymentSummaryResponse;
import com.example.wmbservice.service.PaymentSummaryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "api.v1.mode=disabled",
        "wmb.auth.password=secret"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiV1DisabledModeIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PaymentSummaryService paymentSummaryService;

    @Test
    void legacyV1RoutesReturnGone() throws Exception {
        mockMvc.perform(get("/api/payment-summary")
                        .param("accounts", "josh")
                        .param("statementPeriod", "MAY2026"))
                .andExpect(status().isGone());

        mockMvc.perform(get("/api/analytics/categories/distinct"))
                .andExpect(status().isGone());

        mockMvc.perform(get("/api/transactions")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31"))
                .andExpect(status().isGone());

        mockMvc.perform(post("/api/cache")
                        .param("cacheKey", "demo")
                        .param("cacheValue", "value"))
                .andExpect(status().isGone());
    }

    @Test
    void v2RoutesRemainOperationalWithJwt() throws Exception {
        when(paymentSummaryService.getPaymentSummary(anyList(), anyString(), anyString()))
                .thenReturn(List.of(new PaymentSummaryResponse(
                        "josh",
                        Map.of("visa", new BigDecimal("20.00")),
                        Map.of("visa", Map.of("groceries", new BigDecimal("20.00")))
                )));

        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(get("/api/v2/payment-summary")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("accounts", "josh")
                        .param("statementPeriod", "MAY2026")
                        .header("X-Transaction-ID", "tx-v2"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Transaction-ID", "tx-v2"))
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"))
                .andExpect(header().doesNotExist("Link"))
                .andExpect(jsonPath("$[0].account").value("josh"))
                .andExpect(jsonPath("$[0].creditCardTotals.visa").value(20.00));
    }

    @Test
    void authRoutesRemainOperational() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    private String loginAndGetAccessToken() throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(body);
        return jsonNode.get("accessToken").asText();
    }
}
