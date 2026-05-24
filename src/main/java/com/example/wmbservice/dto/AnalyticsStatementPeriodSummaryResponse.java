package com.example.wmbservice.dto;

import com.example.wmbservice.model.BudgetTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsStatementPeriodSummaryResponse {
    private String statementPeriod;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private BigDecimal totalAmount;
    private long transactionCount;
    private BigDecimal essentialAmount;
    private long essentialCount;
    private BigDecimal nonessentialAmount;
    private long nonessentialCount;
    /**
     * Account-keyed breakdowns. Keys are BudgetTransaction.account.
     */
    private Map<String, List<AnalyticsCategoryBreakdownResponse>> categoryBreakdown;
    private Map<String, List<AnalyticsCriticalityBreakdownResponse>> criticalityBreakdown;
    private Map<String, AnalyticsAccountBreakdownResponse> accountBreakdown;
    private Map<String, List<AnalyticsPaymentMethodBreakdownResponse>> paymentMethodBreakdown;
    private Map<String, List<BudgetTransaction>> outliers;
    private LocalDateTime generatedAt;
}

