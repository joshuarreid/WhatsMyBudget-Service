package com.example.wmbservice.service;

import com.example.wmbservice.dto.*;
import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.repository.BudgetTransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Service layer for analytics.
 *
 * Contract:
 * - Inputs: statementPeriod + optional filters.
 * - Outputs: JSON-friendly POJOs/Maps.
 * - No writes.
 */
@Service
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

    private final BudgetTransactionRepository budgetTransactionRepository;

    public AnalyticsService(BudgetTransactionRepository budgetTransactionRepository) {
        this.budgetTransactionRepository = budgetTransactionRepository;
    }

    @Transactional
    public AnalyticsPeriodsResponse getAllPeriods(String transactionId) {
        logger.info("getAllPeriods called. transactionId={}", transactionId);
        // Minimal version: just distinct periods from actuals.
        // Later we can enrich with per-period totals.
        List<String> periods = budgetTransactionRepository.findDistinctStatementPeriods();
        return new AnalyticsPeriodsResponse(periods, periods.size());
    }

    @Transactional
    public AnalyticsPeriodOverviewResponse getPeriodOverview(String period, String paymentMethod, String account, String transactionId) {
        logger.info("getPeriodOverview called. transactionId={}, period={}, paymentMethod={}, account={}", transactionId, period, paymentMethod, account);
        Object[] row = budgetTransactionRepository.getOverviewTotals(period, blankToNull(paymentMethod), blankToNull(account));
        BigDecimal total = (BigDecimal) row[0];
        Long count = (Long) row[1];
        return new AnalyticsPeriodOverviewResponse(period, blankToNull(paymentMethod), blankToNull(account), total, count);
    }

    @Transactional
    public List<AnalyticsCategoryBreakdownResponse> getCategoryBreakdown(String period, String paymentMethod, String account, String transactionId) {
        logger.info("getCategoryBreakdown called. transactionId={}, period={}, paymentMethod={}, account={}", transactionId, period, paymentMethod, account);
        List<Object[]> rows = budgetTransactionRepository.getCategoryBreakdown(period, blankToNull(paymentMethod), blankToNull(account));
        List<AnalyticsCategoryBreakdownResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new AnalyticsCategoryBreakdownResponse((String) r[0], (BigDecimal) r[1], (Long) r[2]));
        }
        return out;
    }

    @Transactional
    public List<AnalyticsCategoryBreakdownResponse> getTopCategories(String period, int limit, String paymentMethod, String account, String transactionId) {
        logger.info("getTopCategories called. transactionId={}, period={}, limit={}, paymentMethod={}, account={}", transactionId, period, limit, paymentMethod, account);
        List<AnalyticsCategoryBreakdownResponse> all = getCategoryBreakdown(period, paymentMethod, account, transactionId);
        if (limit <= 0) return List.of();
        return all.subList(0, Math.min(limit, all.size()));
    }

    @Transactional
    public List<AnalyticsAccountBreakdownResponse> getAccountBreakdown(String period, String paymentMethod, String transactionId) {
        logger.info("getAccountBreakdown called. transactionId={}, period={}, paymentMethod={}", transactionId, period, paymentMethod);
        List<Object[]> rows = budgetTransactionRepository.getAccountBreakdown(period, blankToNull(paymentMethod));
        List<AnalyticsAccountBreakdownResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new AnalyticsAccountBreakdownResponse((String) r[0], (BigDecimal) r[1], (Long) r[2]));
        }
        return out;
    }

    @Transactional
    public List<AnalyticsPaymentMethodBreakdownResponse> getPaymentMethodBreakdown(String period, String account, String transactionId) {
        logger.info("getPaymentMethodBreakdown called. transactionId={}, period={}, account={}", transactionId, period, account);
        List<Object[]> rows = budgetTransactionRepository.getPaymentMethodBreakdown(period, blankToNull(account));
        List<AnalyticsPaymentMethodBreakdownResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new AnalyticsPaymentMethodBreakdownResponse((String) r[0], (BigDecimal) r[1], (Long) r[2]));
        }
        return out;
    }

    @Transactional
    public List<AnalyticsDailyTotalResponse> getDailyTotals(String period, String paymentMethod, String account, String transactionId) {
        logger.info("getDailyTotals called. transactionId={}, period={}, paymentMethod={}, account={}", transactionId, period, paymentMethod, account);
        List<Object[]> rows = budgetTransactionRepository.getDailyTotals(period, blankToNull(paymentMethod), blankToNull(account));
        List<AnalyticsDailyTotalResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new AnalyticsDailyTotalResponse((LocalDate) r[0], (BigDecimal) r[1], (Long) r[2]));
        }
        return out;
    }

    @Transactional
    public List<AnalyticsCriticalityBreakdownResponse> getCriticalityBreakdown(String period, String paymentMethod, String account, String transactionId) {
        logger.info("getCriticalityBreakdown called. transactionId={}, period={}, paymentMethod={}, account={}", transactionId, period, paymentMethod, account);
        List<Object[]> rows = budgetTransactionRepository.getCriticalityBreakdown(period, blankToNull(paymentMethod), blankToNull(account));
        List<AnalyticsCriticalityBreakdownResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new AnalyticsCriticalityBreakdownResponse((String) r[0], (BigDecimal) r[1], (Long) r[2]));
        }
        return out;
    }

    @Transactional
    public List<AnalyticsDuplicateResponse> getDuplicates(String period, String transactionId) {
        logger.info("getDuplicates called. transactionId={}, period={}", transactionId, period);
        List<Object[]> rows = budgetTransactionRepository.findDuplicatesByRowHash(period);
        List<AnalyticsDuplicateResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new AnalyticsDuplicateResponse((String) r[0], (Long) r[1], (BigDecimal) r[2]));
        }
        return out;
    }

    @Transactional
    public List<BudgetTransaction> getUncategorized(String period, String transactionId) {
        logger.info("getUncategorized called. transactionId={}, period={}", transactionId, period);
        return budgetTransactionRepository.findUncategorized(period);
    }

    @Transactional
    public List<BudgetTransaction> getOutliers(String period, int limit, String transactionId) {
        logger.info("getOutliers called. transactionId={}, period={}, limit={}", transactionId, period, limit);
        int safeLimit = Math.max(0, Math.min(limit, 500));
        if (safeLimit == 0) return List.of();
        return budgetTransactionRepository.findTopByStatementPeriodOrderByAmountDesc(period, PageRequest.of(0, safeLimit));
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
