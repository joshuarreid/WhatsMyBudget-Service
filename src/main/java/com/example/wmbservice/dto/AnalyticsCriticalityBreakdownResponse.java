package com.example.wmbservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for criticality breakdown analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsCriticalityBreakdownResponse {
    private String criticality;
    private BigDecimal totalAmount;
    private long transactionCount;
}

