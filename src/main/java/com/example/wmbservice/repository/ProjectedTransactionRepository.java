package com.example.wmbservice.repository;

import com.example.wmbservice.model.ProjectedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ProjectedTransaction CRUD and queries.
 * Provides filter and business-key lookups used by the service layer.
 */
@Repository
public interface ProjectedTransactionRepository extends JpaRepository<ProjectedTransaction, Long> {
    /**
     * Filter projected transactions by optional fields. If a field is null, it is not used as a filter.
     * Results are ordered by createdTime descending.
     */
    @Query("SELECT t FROM ProjectedTransaction t WHERE (:statementPeriod IS NULL OR t.statementPeriod = :statementPeriod) " +
            "AND (:account IS NULL OR LOWER(t.account) = LOWER(:account)) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:criticality IS NULL OR t.criticality = :criticality) " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "ORDER BY t.createdTime DESC")
    List<ProjectedTransaction> findByFilters(@Param("statementPeriod") String statementPeriod,
                                             @Param("account") String account,
                                             @Param("category") String category,
                                             @Param("criticality") String criticality,
                                             @Param("paymentMethod") String paymentMethod);

    /**
     * Filter projected transactions by inclusive transactionDate range and optional fields.
     * Excludes rows with null transactionDate.
     */
    @Query("SELECT t FROM ProjectedTransaction t WHERE t.transactionDate IS NOT NULL " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (:account IS NULL OR LOWER(t.account) = LOWER(:account)) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:criticality IS NULL OR t.criticality = :criticality) " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "ORDER BY t.createdTime DESC")
    List<ProjectedTransaction> findByDateRangeFilters(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     @Param("account") String account,
                                                     @Param("category") String category,
                                                     @Param("criticality") String criticality,
                                                     @Param("paymentMethod") String paymentMethod);

    /**
     * Find a projected transaction by business-key columns (same semantics as row_hash lookup,
     * but implemented by matching columns directly). This avoids requiring a row_hash column in the DB.
     *
     * Note: transactionDate may be null. Query matches nulls explicitly.
     */
    @Query("SELECT t FROM ProjectedTransaction t WHERE " +
            "t.name = :name AND LOWER(t.account) = LOWER(:account) AND t.amount = :amount AND t.category = :category " +
            "AND t.criticality = :criticality AND " +
            "((:transactionDate IS NULL AND t.transactionDate IS NULL) OR t.transactionDate = :transactionDate) " +
            "AND t.paymentMethod = :paymentMethod AND t.statementPeriod = :statementPeriod")
    Optional<ProjectedTransaction> findByBusinessKey(@Param("name") String name,
                                                     @Param("account") String account,
                                                     @Param("amount") BigDecimal amount,
                                                     @Param("category") String category,
                                                     @Param("criticality") String criticality,
                                                     @Param("transactionDate") LocalDate transactionDate,
                                                     @Param("paymentMethod") String paymentMethod,
                                                     @Param("statementPeriod") String statementPeriod);
}