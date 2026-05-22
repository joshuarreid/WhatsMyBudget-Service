package com.example.wmbservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for payment method breakdown analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsPaymentMethodBreakdownResponse {
    private String paymentMethod;
    private BigDecimal totalAmount;
    private long transactionCount;
}

