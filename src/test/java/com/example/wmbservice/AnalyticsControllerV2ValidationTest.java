package com.example.wmbservice;

import com.example.wmbservice.controller.AnalyticsControllerV2;
import com.example.wmbservice.dto.AnalyticsPeriodOverviewResponse;
import com.example.wmbservice.dto.AnalyticsPeriodsResponse;
import com.example.wmbservice.service.AnalyticsService;
import com.example.wmbservice.service.StatementPeriodSummaryService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerV2ValidationTest {

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private StatementPeriodSummaryService statementPeriodSummaryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AnalyticsControllerV2 controller = new AnalyticsControllerV2(analyticsService, statementPeriodSummaryService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllPeriods_withoutTransactionId_generatesAndEchoesHeader() throws Exception {
        when(analyticsService.getAllPeriods(anyString()))
                .thenReturn(new AnalyticsPeriodsResponse(List.of("MAY2026"), 1));

        String headerValue = mockMvc.perform(get("/api/v2/analytics/periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(header().exists("X-Transaction-ID"))
                .andReturn()
                .getResponse()
                .getHeader("X-Transaction-ID");

        assertThat(headerValue).isNotBlank();

        ArgumentCaptor<String> txCaptor = ArgumentCaptor.forClass(String.class);
        verify(analyticsService).getAllPeriods(txCaptor.capture());
        assertThat(txCaptor.getValue()).isEqualTo(headerValue);
    }

    @Test
    void getDateRangeOverview_invalidDate_returns400() throws Exception {
        mockMvc.perform(get("/api/v2/analytics/range/overview")
                        .param("startDate", "bad-date")
                        .param("endDate", "2026-05-31")
                        .header("X-Transaction-ID", "tx-bad-date"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Transaction-ID", "tx-bad-date"));
    }

    @Test
    void getDateRangeOverview_validRange_callsServiceWithParsedDates() throws Exception {
        when(analyticsService.getDateRangeOverview(any(LocalDate.class), any(LocalDate.class), any(), any(), anyString()))
                .thenReturn(new AnalyticsPeriodOverviewResponse(null, null, null, new BigDecimal("30.00"), 2));

        mockMvc.perform(get("/api/v2/analytics/range/overview")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .header("X-Transaction-ID", "tx-range"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(30.00))
                .andExpect(jsonPath("$.transactionCount").value(2))
                .andExpect(header().string("X-Transaction-ID", "tx-range"));

        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(analyticsService).getDateRangeOverview(startCaptor.capture(), endCaptor.capture(), any(), any(), anyString());
        assertThat(startCaptor.getValue()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(endCaptor.getValue()).isEqualTo(LocalDate.of(2026, 5, 31));
    }
}

