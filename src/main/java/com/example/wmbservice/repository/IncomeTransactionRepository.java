package com.example.wmbservice.repository;

import com.example.wmbservice.model.IncomeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeTransactionRepository extends JpaRepository<IncomeTransaction, Long> {

    @Query("SELECT t FROM IncomeTransaction t WHERE (:statementPeriod IS NULL OR t.statementPeriod = :statementPeriod) " +
            "AND (:account IS NULL OR t.account = :account) " +
            "AND (:recurringMonthly IS NULL OR t.recurringMonthly = :recurringMonthly) " +
            "ORDER BY t.transactionDate DESC")
    List<IncomeTransaction> findByFilters(@Param("statementPeriod") String statementPeriod,
                                          @Param("account") String account,
                                          @Param("recurringMonthly") Boolean recurringMonthly);

    @Query("SELECT t FROM IncomeTransaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (:account IS NULL OR t.account = :account) " +
            "AND (:recurringMonthly IS NULL OR t.recurringMonthly = :recurringMonthly) " +
            "ORDER BY t.transactionDate DESC")
    List<IncomeTransaction> findByDateRangeFilters(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("account") String account,
                                                   @Param("recurringMonthly") Boolean recurringMonthly);
}
