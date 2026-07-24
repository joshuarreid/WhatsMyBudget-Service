package com.example.wmbservice;

import com.example.wmbservice.controller.IncomeTransactionControllerV2;
import com.example.wmbservice.model.IncomeTransaction;
import com.example.wmbservice.model.IncomeTransactionList;
import com.example.wmbservice.service.IncomeTransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IncomeTransactionControllerV2ContractTest {

    @Mock
    private IncomeTransactionService incomeTransactionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        IncomeTransactionControllerV2 controller = new IncomeTransactionControllerV2(incomeTransactionService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    @Test
    void listIncomeTransactions_returns200_count_total_andEchoesTransactionId() throws Exception {
        IncomeTransaction first = sampleIncome("Paycheck", "1000.00", LocalDate.of(2026, 5, 10));
        IncomeTransaction second = sampleIncome("Bonus", "250.00", LocalDate.of(2026, 5, 20));
        when(incomeTransactionService.getTransactions(any(), any(), any(), anyString()))
                .thenReturn(new IncomeTransactionList(List.of(first, second)));

        mockMvc.perform(get("/api/v2/income-transactions")
                        .header("X-Transaction-ID", "tx-income-list"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Transaction-ID", "tx-income-list"))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.total").value(1250.00))
                .andExpect(jsonPath("$.transactions[0].description").value("Paycheck"));
    }

    @Test
    void createIncomeTransaction_returns201_body_andEchoesTransactionId() throws Exception {
        IncomeTransaction created = sampleIncome("Paycheck", "2500.00", LocalDate.of(2026, 5, 1));
        created.setId(1L);

        when(incomeTransactionService.createTransaction(any(IncomeTransaction.class), anyString()))
                .thenReturn(created);

        IncomeTransaction request = sampleIncome("Paycheck", "2500.00", LocalDate.of(2026, 5, 1));

        mockMvc.perform(post("/api/v2/income-transactions")
                        .header("X-Transaction-ID", "tx-income-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Transaction-ID", "tx-income-create"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Paycheck"))
                .andExpect(jsonPath("$.amount").value(2500.00))
                .andExpect(jsonPath("$.statementPeriod").value("MAY2026"));
    }

    private static IncomeTransaction sampleIncome(String description, String amount, LocalDate transactionDate) {
        IncomeTransaction tx = new IncomeTransaction();
        tx.setDescription(description);
        tx.setAmount(new BigDecimal(amount));
        tx.setTransactionDate(transactionDate);
        tx.setAccount("josh");
        tx.setStatementPeriod("MAY2026");
        tx.setRecurringMonthly(false);
        tx.setCreatedTime(LocalDateTime.of(2026, 5, 1, 12, 0));
        tx.setUpdatedTime(LocalDateTime.of(2026, 5, 1, 12, 0));
        return tx;
    }
}
