package com.example.wmbservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "statement_period_summaries",
        indexes = {
                @Index(name = "idx_statement_period_summary_start_date", columnList = "period_start_date"),
                @Index(name = "idx_statement_period_summary_end_date", columnList = "period_end_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uniq_statement_period_summary_period", columnNames = "statement_period")
        }
)
public class StatementPeriodSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_period", nullable = false, unique = true, length = 32)
    private String statementPeriod;

    @Column(name = "period_start_date")
    private LocalDate periodStartDate;

    @Column(name = "period_end_date")
    private LocalDate periodEndDate;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "transaction_count", nullable = false)
    private Long transactionCount;

    @Column(name = "essential_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal essentialAmount;

    @Column(name = "essential_count", nullable = false)
    private Long essentialCount;

    @Column(name = "nonessential_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal nonessentialAmount;

    @Column(name = "nonessential_count", nullable = false)
    private Long nonessentialCount;

    @Lob
    @Column(name = "category_breakdown_json", columnDefinition = "LONGTEXT")
    private String categoryBreakdownJson;

    @Lob
    @Column(name = "criticality_breakdown_json", columnDefinition = "LONGTEXT")
    private String criticalityBreakdownJson;

    @Lob
    @Column(name = "account_breakdown_json", columnDefinition = "LONGTEXT")
    private String accountBreakdownJson;

    @Lob
    @Column(name = "payment_method_breakdown_json", columnDefinition = "LONGTEXT")
    private String paymentMethodBreakdownJson;

    @Lob
    @Column(name = "outliers_json", columnDefinition = "LONGTEXT")
    private String outliersJson;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}

