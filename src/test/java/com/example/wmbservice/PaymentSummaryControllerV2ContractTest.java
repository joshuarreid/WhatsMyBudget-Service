package com.example.wmbservice;

import com.example.wmbservice.controller.PaymentSummaryControllerV2;
import com.example.wmbservice.model.PaymentSummaryResponse;
import com.example.wmbservice.service.PaymentSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentSummaryControllerV2ContractTest {

    @Mock
    private PaymentSummaryService paymentSummaryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PaymentSummaryControllerV2 controller = new PaymentSummaryControllerV2(paymentSummaryService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getPaymentSummary_byPeriod_trimsAccounts_andEchoesTransactionId() throws Exception {
        when(paymentSummaryService.getPaymentSummary(anyList(), anyString(), anyString()))
                .thenReturn(List.of(new PaymentSummaryResponse(
                        "josh",
                        Map.of("visa", new BigDecimal("20.00")),
                        Map.of("visa", Map.of("groceries", new BigDecimal("20.00")))
                )));

        mockMvc.perform(get("/api/v2/payment-summary")
                        .param("accounts", " josh, , anna ")
                        .param("statementPeriod", "MAY2026")
                        .header("X-Transaction-ID", "tx-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Transaction-ID", "tx-123"))
                .andExpect(jsonPath("$[0].account").value("josh"))
                .andExpect(jsonPath("$[0].creditCardTotals.visa").value(20.00));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> accountsCaptor = ArgumentCaptor.forClass(List.class);
        verify(paymentSummaryService).getPaymentSummary(accountsCaptor.capture(), anyString(), anyString());
        assertThat(accountsCaptor.getValue()).containsExactly("josh", "anna");
    }

    @Test
    void getPaymentSummary_blankStatementPeriod_returns400_andEchoesTransactionId() throws Exception {
        mockMvc.perform(get("/api/v2/payment-summary")
                        .param("accounts", "josh")
                        .param("statementPeriod", " ")
                        .header("X-Transaction-ID", "tx-400"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Transaction-ID", "tx-400"));
    }

    @Test
    void getPaymentSummary_byDateRange_invalidDate_returns400_andEchoesTransactionId() throws Exception {
        mockMvc.perform(get("/api/v2/payment-summary")
                        .param("accounts", "josh")
                        .param("startDate", "not-a-date")
                        .param("endDate", "2026-05-31")
                        .header("X-Transaction-ID", "tx-date"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Transaction-ID", "tx-date"));
    }

    @Test
    void getPaymentSummary_byDateRange_trimsAccounts_andPassesParsedDates() throws Exception {
        when(paymentSummaryService.getPaymentSummaryByDateRange(anyList(), any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v2/payment-summary")
                        .param("accounts", " josh, , anna ")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .header("X-Transaction-ID", "tx-range"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Transaction-ID", "tx-range"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> accountsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(paymentSummaryService).getPaymentSummaryByDateRange(accountsCaptor.capture(), startCaptor.capture(), endCaptor.capture(), anyString());
        assertThat(accountsCaptor.getValue()).containsExactly("josh", "anna");
        assertThat(startCaptor.getValue()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(endCaptor.getValue()).isEqualTo(LocalDate.of(2026, 5, 31));
    }
}


