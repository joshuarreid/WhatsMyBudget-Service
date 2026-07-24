package com.example.wmbservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "income_transactions",
        indexes = {
                @Index(name = "idx_income_statement_period", columnList = "statement_period"),
                @Index(name = "idx_income_transaction_date", columnList = "transaction_date"),
                @Index(name = "idx_income_account", columnList = "account"),
                @Index(name = "idx_income_recurring", columnList = "is_recurring_monthly")
        }
)
public class IncomeTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String description;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 12, fraction = 2)
    @Column(nullable = false)
    private BigDecimal amount;

    @NotNull
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @NotBlank
    @Size(max = 32)
    @Column(nullable = false)
    private String account;

    @NotBlank
    @Size(max = 32)
    @Column(name = "statement_period", nullable = false)
    private String statementPeriod;

    @NotNull
    @Column(name = "is_recurring_monthly", nullable = false)
    private Boolean recurringMonthly = false;

    @Size(max = 7)
    @Column(name = "recurrence_start_month")
    private String recurrenceStartMonth;

    @Size(max = 7)
    @Column(name = "recurrence_end_month")
    private String recurrenceEndMonth;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;
}
