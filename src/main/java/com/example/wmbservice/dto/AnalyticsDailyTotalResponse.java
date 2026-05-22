package com.example.wmbservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for daily totals analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDailyTotalResponse {
    private LocalDate date;
    private BigDecimal totalAmount;
    private long transactionCount;
}

