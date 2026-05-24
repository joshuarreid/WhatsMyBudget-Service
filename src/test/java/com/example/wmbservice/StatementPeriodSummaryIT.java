package com.example.wmbservice;

import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.model.StatementPeriod;
import com.example.wmbservice.repository.BudgetTransactionRepository;
import com.example.wmbservice.repository.StatementPeriodRepository;
import com.example.wmbservice.repository.StatementPeriodSummaryRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatementPeriodSummaryIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BudgetTransactionRepository budgetTransactionRepository;

    @Autowired
    StatementPeriodRepository statementPeriodRepository;

    @Autowired
    StatementPeriodSummaryRepository statementPeriodSummaryRepository;

    @BeforeEach
    void setup() {
        statementPeriodSummaryRepository.deleteAll();
        budgetTransactionRepository.deleteAll();
        statementPeriodRepository.deleteAll();

        statementPeriodRepository.save(period("JANUARY2020", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)));
        statementPeriodRepository.save(period("FEBRUARY2020", LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29)));
        statementPeriodRepository.save(period("MAY2030", LocalDate.of(2030, 5, 1), LocalDate.of(2030, 5, 31)));

        budgetTransactionRepository.save(tx("Rent", "100.00", "housing", "Essential", LocalDate.of(2020, 1, 3), "josh", "visa", "JANUARY2020", "s1"));
        budgetTransactionRepository.save(tx("Coffee", "12.00", "dining", "Nonessential", LocalDate.of(2020, 1, 10), "josh", "visa", "JANUARY2020", "s2"));
        budgetTransactionRepository.save(tx("Groceries", "40.00", "groceries", "Essential", LocalDate.of(2020, 2, 5), "joint", "visa", "FEBRUARY2020", "s3"));
        budgetTransactionRepository.save(tx("Shoes", "25.00", "clothing", "Nonessential", LocalDate.of(2020, 2, 15), "josh", "amex", "FEBRUARY2020", "s4"));
        budgetTransactionRepository.save(tx("Future", "50.00", "travel", "Nonessential", LocalDate.of(2030, 5, 20), "josh", "amex", "MAY2030", "s5"));
    }

    private static StatementPeriod period(String name, LocalDate start, LocalDate end) {
        StatementPeriod p = new StatementPeriod();
        p.setPeriodName(name);
        p.setStartDate(start);
        p.setEndDate(end);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    private static BudgetTransaction tx(String name,
                                        String amount,
                                        String category,
                                        String criticality,
                                        LocalDate date,
                                        String account,
                                        String paymentMethod,
                                        String period,
                                        String rowHash) {
        BudgetTransaction t = new BudgetTransaction();
        t.setName(name);
        t.setAmount(new BigDecimal(amount));
        t.setCategory(category);
        t.setCriticality(criticality);
        t.setTransactionDate(date);
        t.setAccount(account);
        t.setPaymentMethod(paymentMethod);
        t.setStatementPeriod(period);
        t.setRowHash(rowHash);
        return t;
    }

    @Test
    void summaryByPeriod_persistsClosedPeriodAndReturnsBreakdowns() throws Exception {
        mockMvc.perform(get("/api/analytics/summaries/FEBRUARY2020"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementPeriod").value("FEBRUARY2020"))
                .andExpect(jsonPath("$.periodStartDate").value("2020-02-05"))
                .andExpect(jsonPath("$.periodEndDate").value("2020-02-15"))
                .andExpect(jsonPath("$.transactionCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(65.00))
                .andExpect(jsonPath("$.essentialAmount").value(40.00))
                .andExpect(jsonPath("$.nonessentialAmount").value(25.00))
                .andExpect(jsonPath("$.categoryBreakdown.joint[0].category").value("groceries"))
                .andExpect(jsonPath("$.categoryBreakdown.joint[0].totalAmount").value(40.00))
                .andExpect(jsonPath("$.categoryBreakdown.joint[0].transactionCount").value(1))
                .andExpect(jsonPath("$.categoryBreakdown.josh[0].category").value("clothing"))
                .andExpect(jsonPath("$.categoryBreakdown.josh[0].totalAmount").value(25.00))
                .andExpect(jsonPath("$.categoryBreakdown.josh[0].transactionCount").value(1))
                .andExpect(jsonPath("$.accountBreakdown.joint.account").value("joint"))
                .andExpect(jsonPath("$.accountBreakdown.joint.totalAmount").value(40.00))
                .andExpect(jsonPath("$.accountBreakdown.joint.transactionCount").value(1))
                .andExpect(jsonPath("$.accountBreakdown.josh.account").value("josh"))
                .andExpect(jsonPath("$.accountBreakdown.josh.totalAmount").value(25.00))
                .andExpect(jsonPath("$.accountBreakdown.josh.transactionCount").value(1))
                .andExpect(jsonPath("$.paymentMethodBreakdown.joint[0].paymentMethod").value("visa"))
                .andExpect(jsonPath("$.paymentMethodBreakdown.josh[0].paymentMethod").value("amex"))
                .andExpect(jsonPath("$.outliers.joint.length()").value(1))
                .andExpect(jsonPath("$.outliers.josh.length()").value(1));

        Assertions.assertThat(statementPeriodSummaryRepository.findByStatementPeriod("FEBRUARY2020")).isPresent();
    }

    @Test
    void summaryRange_returnsInclusiveSortedSummaries() throws Exception {
        mockMvc.perform(get("/api/analytics/summaries")
                        .param("startPeriod", "JANUARY2020")
                        .param("endPeriod", "FEBRUARY2020"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statementPeriod").value("JANUARY2020"))
                .andExpect(jsonPath("$[1].statementPeriod").value("FEBRUARY2020"));
    }

    @Test
    void summaryByOpenPeriod_returnsTransientSummaryWithoutPersisting() throws Exception {
        mockMvc.perform(get("/api/analytics/summaries/MAY2030"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementPeriod").value("MAY2030"))
                .andExpect(jsonPath("$.periodStartDate").value("2030-05-20"))
                .andExpect(jsonPath("$.periodEndDate").value("2030-05-20"))
                .andExpect(jsonPath("$.transactionCount").value(1))
                .andExpect(jsonPath("$.totalAmount").value(50.00));

        Assertions.assertThat(statementPeriodSummaryRepository.findByStatementPeriod("MAY2030")).isEmpty();
    }
}

