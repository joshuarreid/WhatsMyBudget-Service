package com.example.wmbservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for account breakdown analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsAccountBreakdownResponse {
    private String account;
    private BigDecimal totalAmount;
    private long transactionCount;
}

