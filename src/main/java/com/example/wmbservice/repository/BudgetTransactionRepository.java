package com.example.wmbservice.repository;

import com.example.wmbservice.model.BudgetTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for BudgetTransaction CRUD and queries.
 * All queries can be logged in the service layer.
 */
@Repository
public interface BudgetTransactionRepository extends JpaRepository<BudgetTransaction, Long> {

    /**
     * Find a transaction by row hash and statement period for deduplication.
     */
    @Query("SELECT t FROM BudgetTransaction t WHERE t.rowHash = :rowHash AND t.statementPeriod = :statementPeriod")
    Optional<BudgetTransaction> findByRowHashAndStatementPeriod(@Param("rowHash") String rowHash,
                                                                @Param("statementPeriod") String statementPeriod);

    /**
     * Filter transactions by optional statementPeriod and other optional fields. If a field is null, it is not used as a filter.
     * statementPeriod is now optional.
     */
    @Query("SELECT t FROM BudgetTransaction t WHERE (:statementPeriod IS NULL OR t.statementPeriod = :statementPeriod) " +
            "AND (:account IS NULL OR t.account = :account) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:criticality IS NULL OR t.criticality = :criticality) " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "ORDER BY t.transactionDate DESC")
    List<BudgetTransaction> findByFilters(@Param("statementPeriod") String statementPeriod,
                                          @Param("account") String account,
                                          @Param("category") String category,
                                          @Param("criticality") String criticality,
                                          @Param("paymentMethod") String paymentMethod);

    /**
     * Filter transactions by transactionDate range and optional fields.
     * Range is inclusive and requires non-null start/end.
     */
    @Query("SELECT t FROM BudgetTransaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (:account IS NULL OR t.account = :account) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:criticality IS NULL OR t.criticality = :criticality) " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "ORDER BY t.transactionDate DESC")
    List<BudgetTransaction> findByDateRangeFilters(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("account") String account,
                                                  @Param("category") String category,
                                                  @Param("criticality") String criticality,
                                                  @Param("paymentMethod") String paymentMethod);

    /**
     * Fetch distinct periods seen in actual transactions.
     */
    @Query("SELECT DISTINCT t.statementPeriod FROM BudgetTransaction t ORDER BY t.statementPeriod DESC")
    List<String> findDistinctStatementPeriods();

    /**
     * Overview totals for a period with optional paymentMethod/account filters.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0), COUNT(t) " +
            "FROM BudgetTransaction t " +
            "WHERE t.statementPeriod = :statementPeriod " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "AND (:account IS NULL OR t.account = :account)")
    Object[] getOverviewTotals(@Param("statementPeriod") String statementPeriod,
                              @Param("paymentMethod") String paymentMethod,
                              @Param("account") String account);

    /**
     * Overview totals for a date range with optional paymentMethod/account filters.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0), COUNT(t) " +
            "FROM BudgetTransaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "AND (:account IS NULL OR t.account = :account)")
    Object[] getOverviewTotalsByDateRange(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("paymentMethod") String paymentMethod,
                                         @Param("account") String account);

    /**
     * Grouped totals for a period by category.
     */
    @Query("SELECT t.category, COALESCE(SUM(t.amount), 0), COUNT(t) " +
            "FROM BudgetTransaction t " +
            "WHERE t.statementPeriod = :statementPeriod " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "AND (:account IS NULL OR t.account = :account) " +
            "GROUP BY t.category " +
            "ORDER BY COALESCE(SUM(t.amount), 0) DESC")
    List<Object[]> getCategoryBreakdown(@Param("statementPeriod") String statementPeriod,
                                        @Param("paymentMethod") String paymentMethod,
                                        @Param("account") String account);

    /**
     * Grouped totals for a date range by category.
     */
    @Query("SELECT t.category, COALESCE(SUM(t.amount), 0), COUNT(t) " +
            "FROM BudgetTransaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "AND (:account IS NULL OR t.account = :account) " +
            "GROUP BY t.category " +
            "ORDER BY COALESCE(SUM(t.amount), 0) DESC")
    List<Object[]> getCategoryBreakdownByDateRange(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("paymentMethod") String paymentMethod,
                                                  @Param("account") String account);

    /**
     * Grouped totals for a period by account.
     */
    @Query("SELECT t.account, COALESCE(SUM(t.amount), 0), COUNT(t) " +
            "FROM BudgetTransaction t " +
            "WHERE t.statementPeriod = :statementPeriod " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "AND (:account IS NULL OR t.account = :account) " +
            "GROUP BY t.account " +
            "ORDER BY COALESCE(SUM(t.amount), 0) DESC")
    List<Object[]> getAccountBreakdown(@Param("statementPeriod") String statementPeriod,
                                       @Param("paymentMethod") String paymentMethod,
                                       @Param("account") String account);

    /**
     * Grouped totals for a date range by account.
     */
    @Query("SELECT t.account, COALESCE(SUM(t.amount), 0), COUNT(t) " +
            "FROM BudgetTransaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "AND (:account IS NULL OR t.account = :account) " +
            "GROUP BY t.account " +
            "ORDER BY COALESCE(SUM(t.amount), 0) DESC")
    List<Object[]> getAccountBreakdownByDateRange(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("paymentMethod") String paymentMethod,
                                                 @Param("account") String account);

    // Backwards-compatible overload: allow callers to use two-arg form (no account filter).
    default List<Object[]> getAccountBreakdown(String statementPeriod, String paymentMethod) {
        return getAccountBreakdown(statementPeriod, paymentMethod, null);
    }

    /**
     * Grouped totals for a period by payment method.
     */
    @Query("SELECT t.paymentMethod, COALESCE(SUM(t.amount), 0), COUNT(t) " +
            "FROM BudgetTransaction t " +
            "WHERE t.statementPeriod = :statementPeriod " +
            "AND (:account IS NULL OR t.account = :account) " +
            "GROUP BY t.paymentMethod " +
            "ORDER BY COALESCE(SUM(t.amount), 0) DESC")
    List<Object[]> getPaymentMethodBreakdown(@Param("statementPeriod") String statementPeriod,
                                             @Param("account") String account);

    /**
     * Grouped totals for a date range by payment method.
     */
    @Query("SELECT t.paymentMethod, COALESCE(SUM(t.amount), 0), COUNT(t) " +
            "FROM BudgetTransaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (:account IS NULL OR t.account = :account) " +
            "GROUP BY t.paymentMethod " +
            "ORDER BY COALESCE(SUM(t.amount), 0) DESC")
    List<Object[]> getPaymentMethodBreakdownByDateRange(@Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate,
                                                       @Param("account") String account);

    /**
     * Daily totals within a period.
     */
    @Query("SELECT t.transactionDate, COALESCE(SUM(t.amount), 0), COUNT(t) " +
            "FROM BudgetTransaction t " +
            "WHERE t.statementPeriod = :statementPeriod " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "AND (:account IS NULL OR t.account = :account) " +
            "GROUP BY t.transactionDate " +
            "ORDER BY t.transactionDate ASC")
    List<Object[]> getDailyTotals(@Param("statementPeriod") String statementPeriod,
                                  @Param("paymentMethod") String paymentMethod,
                                  @Param("account") String account);

    /**
     * Daily totals within a date range.
     */
    @Query("SELECT t.transactionDate, COALESCE(SUM(t.amount), 0), COUNT(t) " +
            "FROM BudgetTransaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "AND (:account IS NULL OR t.account = :account) " +
            "GROUP BY t.transactionDate " +
            "ORDER BY t.transactionDate ASC")
    List<Object[]> getDailyTotalsByDateRange(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("paymentMethod") String paymentMethod,
                                            @Param("account") String account);

    /**
     * Grouped totals for a period by criticality.
     */
    @Query("SELECT t.criticality, COALESCE(SUM(t.amount), 0), COUNT(t) " +
            "FROM BudgetTransaction t " +
            "WHERE t.statementPeriod = :statementPeriod " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "AND (:account IS NULL OR t.account = :account) " +
            "GROUP BY t.criticality " +
            "ORDER BY COALESCE(SUM(t.amount), 0) DESC")
    List<Object[]> getCriticalityBreakdown(@Param("statementPeriod") String statementPeriod,
                                           @Param("paymentMethod") String paymentMethod,
                                           @Param("account") String account);

    /**
     * Grouped totals for a date range by criticality.
     */
    @Query("SELECT t.criticality, COALESCE(SUM(t.amount), 0), COUNT(t) " +
            "FROM BudgetTransaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (:paymentMethod IS NULL OR t.paymentMethod = :paymentMethod) " +
            "AND (:account IS NULL OR t.account = :account) " +
            "GROUP BY t.criticality " +
            "ORDER BY COALESCE(SUM(t.amount), 0) DESC")
    List<Object[]> getCriticalityBreakdownByDateRange(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     @Param("paymentMethod") String paymentMethod,
                                                     @Param("account") String account);

    /**
     * Duplicate hashes in a period.
     */
    @Query("SELECT t.rowHash, COUNT(t), COALESCE(SUM(t.amount), 0) " +
            "FROM BudgetTransaction t " +
            "WHERE t.statementPeriod = :statementPeriod AND t.rowHash IS NOT NULL " +
            "GROUP BY t.rowHash " +
            "HAVING COUNT(t) > 1 " +
            "ORDER BY COUNT(t) DESC")
    List<Object[]> findDuplicatesByRowHash(@Param("statementPeriod") String statementPeriod);

    /**
     * Duplicate hashes in a date range.
     */
    @Query("SELECT t.rowHash, COUNT(t), COALESCE(SUM(t.amount), 0) " +
            "FROM BudgetTransaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate AND t.rowHash IS NOT NULL " +
            "GROUP BY t.rowHash " +
            "HAVING COUNT(t) > 1 " +
            "ORDER BY COUNT(t) DESC")
    List<Object[]> findDuplicatesByRowHashByDateRange(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    /**
     * Transactions considered uncategorized.
     *
     * Assumption: category can be NULL/blank or set to common placeholders.
     */
    @Query("SELECT t FROM BudgetTransaction t " +
            "WHERE t.statementPeriod = :statementPeriod " +
            "AND (t.category IS NULL OR TRIM(t.category) = '' OR UPPER(t.category) IN ('UNCATEGORIZED','UNKNOWN','NA','N/A')) " +
            "ORDER BY t.transactionDate DESC")
    List<BudgetTransaction> findUncategorized(@Param("statementPeriod") String statementPeriod);

    /**
     * Uncategorized transactions in a date range.
     */
    @Query("SELECT t FROM BudgetTransaction t " +
            "WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (t.category IS NULL OR TRIM(t.category) = '' OR UPPER(t.category) IN ('UNCATEGORIZED','UNKNOWN','NA','N/A')) " +
            "ORDER BY t.transactionDate DESC")
    List<BudgetTransaction> findUncategorizedByDateRange(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    /**
     * Largest transactions for a period.
     */
    List<BudgetTransaction> findTopByStatementPeriodOrderByAmountDesc(String statementPeriod,
                                                                      org.springframework.data.domain.Pageable pageable);

    /**
     * Largest transactions for a date range.
     */
    List<BudgetTransaction> findByTransactionDateBetweenOrderByAmountDesc(LocalDate startDate,
                                                                          LocalDate endDate,
                                                                          org.springframework.data.domain.Pageable pageable);


}
