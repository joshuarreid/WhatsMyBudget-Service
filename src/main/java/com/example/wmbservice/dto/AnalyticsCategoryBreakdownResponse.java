package com.example.wmbservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for category breakdown analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsCategoryBreakdownResponse {
    private String category;
    private BigDecimal totalAmount;
    private long transactionCount;
}

