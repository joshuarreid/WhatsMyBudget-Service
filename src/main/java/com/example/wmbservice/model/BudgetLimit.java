package com.example.wmbservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a user's budget limits for a given statement period.
 * Maps to the 'budget_limits' table. One row per (account, statement_period).
 * Null limit columns mean "no limit set" (unconstrained).
 */
@Getter
@Setter
@Entity
@Table(
        name = "budget_limits",
        indexes = {
                @Index(name = "idx_budget_limits_account", columnList = "account"),
                @Index(name = "idx_budget_limits_statement_period", columnList = "statement_period")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uniq_budget_limits_account_period", columnNames = {"account", "statement_period"})
        }
)
public class BudgetLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 64)
    @Column(name = "account", nullable = false, length = 64)
    private String account;

    @NotBlank
    @Size(max = 32)
    @Column(name = "statement_period", nullable = false, length = 32)
    private String statementPeriod;

    @Column(name = "essential_limit", precision = 12, scale = 2)
    private BigDecimal essentialLimit;

    @Column(name = "nonessential_limit", precision = 12, scale = 2)
    private BigDecimal nonessentialLimit;

    @Column(name = "total_limit", precision = 12, scale = 2)
    private BigDecimal totalLimit;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

