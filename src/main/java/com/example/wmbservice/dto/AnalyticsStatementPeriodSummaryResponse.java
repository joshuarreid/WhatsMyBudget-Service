package com.example.wmbservice.dto;

import com.example.wmbservice.model.BudgetTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private List<AnalyticsCategoryBreakdownResponse> categoryBreakdown;
    private List<AnalyticsCriticalityBreakdownResponse> criticalityBreakdown;
    private List<AnalyticsAccountBreakdownResponse> accountBreakdown;
    private List<AnalyticsPaymentMethodBreakdownResponse> paymentMethodBreakdown;
    private List<BudgetTransaction> outliers;
    private LocalDateTime generatedAt;
}

