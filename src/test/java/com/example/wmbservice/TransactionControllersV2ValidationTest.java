package com.example.wmbservice;

import com.example.wmbservice.controller.BudgetTransactionControllerV2;
import com.example.wmbservice.controller.ProjectedTransactionControllerV2;
import com.example.wmbservice.service.BudgetTransactionService;
import com.example.wmbservice.service.ProjectedTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllersV2ValidationTest {

    @Mock
    private BudgetTransactionService budgetTransactionService;

    @Mock
    private ProjectedTransactionService projectedTransactionService;

    private MockMvc budgetMockMvc;
    private MockMvc projectedMockMvc;

    @BeforeEach
    void setUp() {
        budgetMockMvc = MockMvcBuilders
                .standaloneSetup(new BudgetTransactionControllerV2(budgetTransactionService))
                .build();
        projectedMockMvc = MockMvcBuilders
                .standaloneSetup(new ProjectedTransactionControllerV2(projectedTransactionService))
                .build();
    }

    @Test
    void budgetTransactions_invalidDateRange_returns400Contract() throws Exception {
        budgetMockMvc.perform(get("/api/v2/transactions")
                        .param("startDate", "2026-05-99")
                        .param("endDate", "2026-05-31")
                        .header("X-Transaction-ID", "tx-budget-bad"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Transaction-ID", "tx-budget-bad"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.transactionId").value("tx-budget-bad"));
    }

    @Test
    void projectedTransactions_invalidDateRange_returns400Contract() throws Exception {
        projectedMockMvc.perform(get("/api/v2/projected-transactions")
                        .param("startDate", "not-a-date")
                        .param("endDate", "2026-05-31")
                        .header("X-Transaction-ID", "tx-projected-bad"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Transaction-ID", "tx-projected-bad"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.transactionId").value("tx-projected-bad"));
    }
}
