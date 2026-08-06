package com.example.wmbservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetLimitResponse {
    private String account;
    private String statementPeriod;
    private BigDecimal essentialLimit;
    private BigDecimal nonessentialLimit;
    private BigDecimal totalLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

