package com.example.wmbservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for period overview analytics (total spend, transaction count, etc).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsPeriodOverviewResponse {
    private String statementPeriod;
    private String paymentMethod;
    private String account;
    private BigDecimal totalAmount;
    private long transactionCount;
}

