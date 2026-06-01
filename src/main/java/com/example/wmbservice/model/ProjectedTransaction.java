package com.example.wmbservice.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a projected transaction used for forecasting.
 * rowHash is transient (not persisted) to avoid requiring a row_hash column in the DB.
 * Service layer computes rowHash for deduplication checks and logging.
 */
@Getter
@Setter
@Entity
@Table(name = "projected_transactions",
        indexes = {
                @Index(name = "idx_statement_period_proj", columnList = "statement_period"),
                @Index(name = "idx_account_proj", columnList = "account"),
                @Index(name = "idx_payment_method_proj", columnList = "payment_method"),
                @Index(name = "idx_category_proj", columnList = "category")
        }
)
public class ProjectedTransaction {
    private static final Logger logger = LoggerFactory.getLogger(ProjectedTransaction.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 12, fraction = 2)
    @Column(nullable = false)
    private BigDecimal amount;

    @NotBlank
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String criticality;

    @JsonProperty("criticality_id")
    @JsonAlias("criticalityId")
    @Column(name = "criticality_id")
    private Long criticalityId;

    @JsonAlias("projectedDate")
    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @NotBlank
    @Column(nullable = false)
    private String account;

    private String status;

    @NotNull
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "payment_method")
    private String paymentMethod;

    @NotBlank
    @Column(name = "statement_period", nullable = false)
    private String statementPeriod;

    /**
     * Deterministic row hash used for deduplication (SHA-256 hex).
     * Marked transient: not persisted to DB to avoid requiring a row_hash column.
     * Service will compute and use this field for logging and in-memory comparison.
     */
    @Transient
    private String rowHash;

    /**
     * Default constructor.
     */
    public ProjectedTransaction() {
        logger.debug("ProjectedTransaction default constructor called");
    }

    /**
     * All-args constructor helpful for tests and manual instantiation.
     */
    public ProjectedTransaction(String name, BigDecimal amount, String category, String criticality,
                                LocalDate transactionDate, String account, String status, LocalDateTime createdTime,
                                String paymentMethod, String statementPeriod, String rowHash) {
        logger.info("ProjectedTransaction instantiated with name={}, amount={}, statementPeriod={}", name, amount, statementPeriod);
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.criticality = criticality;
        this.transactionDate = transactionDate;
        this.account = account;
        this.status = status;
        this.createdTime = createdTime;
        this.paymentMethod = paymentMethod;
        this.statementPeriod = statementPeriod;
        this.rowHash = rowHash;
    }

    @Override
    public boolean equals(Object o) {
        logger.debug("equals called for ProjectedTransaction");
        if (this == o) return true;
        if (!(o instanceof ProjectedTransaction that)) return false;
        return Objects.equals(rowHash, that.rowHash) &&
                Objects.equals(statementPeriod, that.statementPeriod);
    }

    @Override
    public int hashCode() {
        logger.debug("hashCode called for ProjectedTransaction");
        return Objects.hash(rowHash, statementPeriod);
    }

    @Override
    public String toString() {
        logger.debug("toString called for ProjectedTransaction");
        return "ProjectedTransaction{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", criticality='" + criticality + '\'' +
                ", criticalityId=" + criticalityId +
                ", transactionDate=" + transactionDate +
                ", account='" + account + '\'' +
                ", status='" + status + '\'' +
                ", createdTime=" + createdTime +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", statementPeriod='" + statementPeriod + '\'' +
                ", rowHash='" + rowHash + '\'' +
                '}';
    }
}
