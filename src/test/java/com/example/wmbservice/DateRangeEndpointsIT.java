package com.example.wmbservice;

import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.repository.BudgetTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DateRangeEndpointsIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BudgetTransactionRepository budgetTransactionRepository;

    @BeforeEach
    void setup() {
        budgetTransactionRepository.deleteAll();

        budgetTransactionRepository.save(tx("Coffee", "coffee", "10.00", LocalDate.of(2026, 5, 1), "josh", "visa", "MAY2026", "h1"));
        budgetTransactionRepository.save(tx("Groceries", "groceries", "20.00", LocalDate.of(2026, 5, 10), "joint", "visa", "MAY2026", "h2"));
        budgetTransactionRepository.save(tx("Gas", "gas", "5.00", LocalDate.of(2026, 6, 1), "josh", "visa", "JUNE2026", "h3"));
    }

    private static BudgetTransaction tx(String name, String category, String amount, LocalDate date, String account, String paymentMethod, String period, String rowHash) {
        BudgetTransaction t = new BudgetTransaction();
        t.setName(name);
        t.setAmount(new BigDecimal(amount));
        t.setCategory(category);
        t.setCriticality("Planned");
        t.setCriticalityId(3L);
        t.setTransactionDate(date);
        t.setAccount(account);
        t.setPaymentMethod(paymentMethod);
        t.setStatementPeriod(period);
        t.setRowHash(rowHash);
        return t;
    }

    @Test
    void analyticsOverviewByDateRange_isInclusiveAndSummed() throws Exception {
        mockMvc.perform(get("/api/analytics/range/overview")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(30.00));
    }

    @Test
    void analyticsOverviewByDateRange_forNonJointAccount_includesHalfOfJoint() throws Exception {
        mockMvc.perform(get("/api/analytics/range/overview")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .param("account", "josh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account").value("josh"))
                .andExpect(jsonPath("$.transactionCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(20.00));
    }

    @Test
    void analyticsCategoryBreakdownByDateRange_forNonJointAccount_includesHalfOfJoint() throws Exception {
        mockMvc.perform(get("/api/analytics/range/categories")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .param("account", "josh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("coffee"))
                .andExpect(jsonPath("$[0].totalAmount").value(10.00))
                .andExpect(jsonPath("$[0].transactionCount").value(1))
                .andExpect(jsonPath("$[1].category").value("groceries"))
                .andExpect(jsonPath("$[1].totalAmount").value(10.00))
                .andExpect(jsonPath("$[1].transactionCount").value(1));
    }

    @Test
    void analyticsPaymentMethodBreakdownByDateRange_forNonJointAccount_includesHalfOfJoint() throws Exception {
        mockMvc.perform(get("/api/analytics/range/payment-methods")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .param("account", "josh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentMethod").value("visa"))
                .andExpect(jsonPath("$[0].totalAmount").value(20.00))
                .andExpect(jsonPath("$[0].transactionCount").value(2));
    }

    @Test
    void analyticsAccountBreakdownByDateRange_appliesHalfOfJointToEachNonJointAccount() throws Exception {
        mockMvc.perform(get("/api/analytics/range/accounts")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].account", hasItem("josh")))
                .andExpect(jsonPath("$[*].account", hasItem("joint")))
                .andExpect(jsonPath("$[?(@.account=='josh')].totalAmount", hasItem(20.00)))
                .andExpect(jsonPath("$[?(@.account=='josh')].transactionCount", hasItem(2)))
                .andExpect(jsonPath("$[?(@.account=='joint')].totalAmount", hasItem(20.00)))
                .andExpect(jsonPath("$[?(@.account=='joint')].transactionCount", hasItem(1)));
    }

    @Test
    void analyticsDailyTotalsByDateRange_forNonJointAccount_includesHalfOfJoint() throws Exception {
        mockMvc.perform(get("/api/analytics/range/daily")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .param("account", "josh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-05-01"))
                .andExpect(jsonPath("$[0].totalAmount").value(10.00))
                .andExpect(jsonPath("$[0].transactionCount").value(1))
                .andExpect(jsonPath("$[1].date").value("2026-05-10"))
                .andExpect(jsonPath("$[1].totalAmount").value(10.00))
                .andExpect(jsonPath("$[1].transactionCount").value(1));
    }

    @Test
    void analyticsCriticalityBreakdownByDateRange_forNonJointAccount_includesHalfOfJoint() throws Exception {
        mockMvc.perform(get("/api/analytics/range/criticality")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .param("account", "josh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].criticality").value("Planned"))
                .andExpect(jsonPath("$[0].totalAmount").value(20.00))
                .andExpect(jsonPath("$[0].transactionCount").value(2));
    }

    @Test
    void transactionsListByDateRange_filtersRows() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.transactions[0].criticality_id").value(3));
    }

    @Test
    void transactionsListByDateRange_filtersByCriticalityId() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .param("criticality_id", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void paymentSummaryByDateRange_splitsJointForJosh() throws Exception {
        mockMvc.perform(get("/api/payment-summary")
                        .param("accounts", "josh")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].account").value("josh"))
                .andExpect(jsonPath("$[0].creditCardTotals.visa").value(20.00));
    }

    @Test
    void distinctCategories_returnsUniqueSortedCategories() throws Exception {
        mockMvc.perform(get("/api/analytics/categories/distinct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("coffee"))
                .andExpect(jsonPath("$[1]").value("gas"))
                .andExpect(jsonPath("$[2]").value("groceries"));
    }
}
