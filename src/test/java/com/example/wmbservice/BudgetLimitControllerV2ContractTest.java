package com.example.wmbservice;

import com.example.wmbservice.controller.BudgetLimitControllerV2;
import com.example.wmbservice.model.BudgetLimit;
import com.example.wmbservice.service.BudgetLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BudgetLimitControllerV2ContractTest {

    @Mock
    private BudgetLimitService budgetLimitService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BudgetLimitControllerV2 controller = new BudgetLimitControllerV2(budgetLimitService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void upsert_success_returnsContract_andEchoesTransactionId() throws Exception {
        when(budgetLimitService.upsert(anyString(), anyString(), any(), any(), any(), anyString()))
                .thenReturn(sample("josh", "", "100.00", "50.00", "150.00"));

        mockMvc.perform(put("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", "june2026")
                        .header("X-Transaction-ID", "tx-upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "essentialLimit": 100.00,
                                  "nonessentialLimit": 50.00,
                                  "totalLimit": 150.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Transaction-ID", "tx-upsert"))
                .andExpect(jsonPath("$.account").value("josh"))
                .andExpect(jsonPath("$.statementPeriod").value(""))
                .andExpect(jsonPath("$.essentialLimit").value(100.0))
                .andExpect(jsonPath("$.nonessentialLimit").value(50.0))
                .andExpect(jsonPath("$.totalLimit").value(150.0));
    }

    @Test
    void upsert_missingTransactionId_generatesHeader_andPassesToService() throws Exception {
        when(budgetLimitService.upsert(anyString(), anyString(), any(), any(), any(), anyString()))
                .thenReturn(sample("josh", "MAY2026", null, null, "220.00"));

        mockMvc.perform(put("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", "MAY2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "totalLimit": 220.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Transaction-ID"))
                .andExpect(jsonPath("$.totalLimit").value(220.0));

        ArgumentCaptor<String> txCaptor = ArgumentCaptor.forClass(String.class);
        verify(budgetLimitService).upsert(anyString(), anyString(), any(), any(), any(), txCaptor.capture());
        assertThat(txCaptor.getValue()).isNotBlank();
    }

    @Test
    void upsert_serviceValidationError_returns400ErrorContract() throws Exception {
        when(budgetLimitService.upsert(anyString(), anyString(), any(), any(), any(), anyString()))
                .thenThrow(new IllegalArgumentException("account must not be blank"));

        mockMvc.perform(put("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", "MAY2026")
                        .header("X-Transaction-ID", "tx-bad-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Transaction-ID", "tx-bad-account"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.transactionId").value("tx-bad-account"));
    }

    @Test
    void upsert_invalidAccountFormat_returns400ErrorContract() throws Exception {
        mockMvc.perform(put("/api/v2/budget-limits/{account}/{statementPeriod}", "josh@home", "MAY2026")
                        .header("X-Transaction-ID", "tx-acct-format")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Transaction-ID", "tx-acct-format"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void upsert_ignoresStatementPeriodOnWrite_returns200Contract() throws Exception {
        when(budgetLimitService.upsert(anyString(), anyString(), any(), any(), any(), anyString()))
                .thenReturn(sample("josh", "", "5.00", "6.00", "11.00"));

        mockMvc.perform(put("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", "anything")
                        .header("X-Transaction-ID", "tx-period-format")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Transaction-ID", "tx-period-format"))
                .andExpect(jsonPath("$.statementPeriod").value(""));
    }

    @Test
    void upsert_invalidTransactionId_regeneratesSafeTransactionId() throws Exception {
        when(budgetLimitService.upsert(anyString(), anyString(), any(), any(), any(), anyString()))
                .thenReturn(sample("josh", "", "5.00", "6.00", "11.00"));

        String unsafeTxId = "invalid tx id";
        mockMvc.perform(put("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", "MAY2026")
                        .header("X-Transaction-ID", unsafeTxId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Transaction-ID"))
                .andExpect(header().string("X-Transaction-ID", org.hamcrest.Matchers.not(unsafeTxId)));

        ArgumentCaptor<String> txCaptor = ArgumentCaptor.forClass(String.class);
        verify(budgetLimitService).upsert(anyString(), anyString(), any(), any(), any(), txCaptor.capture());
        assertThat(txCaptor.getValue()).doesNotContain(" ");
    }

    @Test
    void upsert_negativeLimit_returns400ErrorContract() throws Exception {
        mockMvc.perform(put("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", "MAY2026")
                        .header("X-Transaction-ID", "tx-neg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "essentialLimit": -1.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Transaction-ID", "tx-neg"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void upsert_moreThanTwoFractionDigits_returns400ErrorContract() throws Exception {
        mockMvc.perform(put("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", "MAY2026")
                        .header("X-Transaction-ID", "tx-scale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "totalLimit": 10.123
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Transaction-ID", "tx-scale"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void upsert_accountLength65_returns400_andSkipsService() throws Exception {
        String longAccount = "a".repeat(65);

        mockMvc.perform(put("/api/v2/budget-limits/{account}/{statementPeriod}", longAccount, "MAY2026")
                        .header("X-Transaction-ID", "tx-account-len")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Transaction-ID", "tx-account-len"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        verifyNoInteractions(budgetLimitService);
    }

    @Test
    void upsert_statementPeriodLength33_isIgnoredOnWrite() throws Exception {
        String longPeriod = "A".repeat(33);
        when(budgetLimitService.upsert(anyString(), anyString(), any(), any(), any(), anyString()))
                .thenReturn(sample("josh", "", null, null, null));

        mockMvc.perform(put("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", longPeriod)
                        .header("X-Transaction-ID", "tx-period-len")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Transaction-ID", "tx-period-len"))
                .andExpect(jsonPath("$.statementPeriod").value(""));
    }

    @Test
    void upsert_totalLimitWith11IntegerDigits_returns400ErrorContract() throws Exception {
        mockMvc.perform(put("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", "MAY2026")
                        .header("X-Transaction-ID", "tx-precision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "totalLimit": 12345678901.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Transaction-ID", "tx-precision"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void upsert_serviceUnexpectedError_returns500Contract() throws Exception {
        when(budgetLimitService.upsert(anyString(), anyString(), any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(put("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", "MAY2026")
                        .header("X-Transaction-ID", "tx-upsert-500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Transaction-ID", "tx-upsert-500"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("UPSERT_ERROR"));
    }

    @Test
    void get_serviceUnexpectedError_returns500Contract() throws Exception {
        when(budgetLimitService.findByAccountAndPeriod(anyString(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", "MAY2026")
                        .header("X-Transaction-ID", "tx-get-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Transaction-ID", "tx-get-500"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("GET_ERROR"));
    }

    @Test
    void list_serviceUnexpectedError_returns500Contract() throws Exception {
        when(budgetLimitService.findByPeriod(anyString()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v2/budget-limits")
                        .param("statementPeriod", "MAY2026")
                        .header("X-Transaction-ID", "tx-list-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Transaction-ID", "tx-list-500"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("LIST_ERROR"));
    }

    @Test
    void getByAccountAndPeriod_found_returns200Contract() throws Exception {
        when(budgetLimitService.findByAccountAndPeriod("josh", "JUNE2026"))
                .thenReturn(Optional.of(sample("josh", "JUNE2026", "10.00", "15.00", "30.00")));

        mockMvc.perform(get("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", "JUNE2026")
                        .header("X-Transaction-ID", "tx-get"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Transaction-ID", "tx-get"))
                .andExpect(jsonPath("$.account").value("josh"))
                .andExpect(jsonPath("$.statementPeriod").value("JUNE2026"));
    }

    @Test
    void getByAccountAndPeriod_notFound_returns404Contract() throws Exception {
        when(budgetLimitService.findByAccountAndPeriod("josh", "JUNE2026")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v2/budget-limits/{account}/{statementPeriod}", "josh", "JUNE2026")
                        .header("X-Transaction-ID", "tx-missing"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Transaction-ID", "tx-missing"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.transactionId").value("tx-missing"));
    }

    @Test
    void listByPeriod_success_returnsArrayContract() throws Exception {
        when(budgetLimitService.findByPeriod("JUNE2026")).thenReturn(List.of(
                sample("josh", "", "10.00", null, "20.00"),
                sample("anna", "", null, "15.00", "25.00")
        ));

        mockMvc.perform(get("/api/v2/budget-limits")
                        .param("statementPeriod", "JUNE2026")
                        .header("X-Transaction-ID", "tx-list"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Transaction-ID", "tx-list"))
                .andExpect(jsonPath("$[0].account").value("josh"))
                .andExpect(jsonPath("$[1].account").value("anna"));
    }

    @Test
    void listByPeriod_withoutStatementPeriod_returnsAllPeriodsContract() throws Exception {
        when(budgetLimitService.findByPeriod(null)).thenReturn(List.of(
                sample("josh", "", "10.00", null, "20.00"),
                sample("anna", "", null, "15.00", "25.00")
        ));

        mockMvc.perform(get("/api/v2/budget-limits")
                        .header("X-Transaction-ID", "tx-list-all"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Transaction-ID", "tx-list-all"))
                .andExpect(jsonPath("$[0].account").value("josh"))
                .andExpect(jsonPath("$[1].account").value("anna"));
    }

    private static BudgetLimit sample(String account,
                                      String period,
                                      String essential,
                                      String nonessential,
                                      String total) {
        BudgetLimit item = new BudgetLimit();
        item.setId(42L);
        item.setAccount(account);
        item.setStatementPeriod(period);
        item.setEssentialLimit(essential == null ? null : new BigDecimal(essential));
        item.setNonessentialLimit(nonessential == null ? null : new BigDecimal(nonessential));
        item.setTotalLimit(total == null ? null : new BigDecimal(total));
        item.setCreatedAt(LocalDateTime.of(2026, 8, 1, 10, 15, 0));
        item.setUpdatedAt(LocalDateTime.of(2026, 8, 1, 11, 30, 0));
        return item;
    }
}
