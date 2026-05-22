package com.example.wmbservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for duplicate transaction analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDuplicateResponse {
    private String rowHash;
    private long occurrences;
    private BigDecimal totalAmount;
}
