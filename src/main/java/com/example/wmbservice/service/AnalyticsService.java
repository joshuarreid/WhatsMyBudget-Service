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
import java.util.UUID;

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

    private static String ensureTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank() || "N/A".equalsIgnoreCase(transactionId)) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return transactionId;
    }

    @Transactional
    public AnalyticsPeriodsResponse getAllPeriods(String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics.svc] -> getAllPeriods txId={}", transactionId);
        // Minimal version: just distinct periods from actuals.
        // Later we can enrich with per-period totals.
        List<String> periods = budgetTransactionRepository.findDistinctStatementPeriods();
        AnalyticsPeriodsResponse resp = new AnalyticsPeriodsResponse(periods, periods.size());
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics.svc] <- getAllPeriods txId={} periods={} durationMs={}", transactionId, resp.count(), ms);
        return resp;
    }

    @Transactional
    public AnalyticsPeriodOverviewResponse getPeriodOverview(String period, String paymentMethod, String account, String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics.svc] -> getPeriodOverview txId={} period={} paymentMethod={} account={}", transactionId, period, paymentMethod, account);
        // Repository signature returns Object[] so cast directly and handle nested array case.
        Object raw = budgetTransactionRepository.getOverviewTotals(period, blankToNull(paymentMethod), blankToNull(account));
        Object[] row;
        if (raw == null) {
            row = new Object[]{null, null};
        } else {
            Object[] arr = (Object[]) raw;
            if (arr.length == 1 && arr[0] instanceof Object[]) {
                row = (Object[]) arr[0];
            } else {
                row = arr;
            }
        }

        Object totalObj = row.length > 0 ? row[0] : null;
        Object countObj = row.length > 1 ? row[1] : null;

        BigDecimal total = toBigDecimal(totalObj);
        Long count = toLong(countObj);
        AnalyticsPeriodOverviewResponse resp = new AnalyticsPeriodOverviewResponse(period, blankToNull(paymentMethod), blankToNull(account), total, count);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics.svc] <- getPeriodOverview txId={} period={} total={} count={} durationMs={}", transactionId, period, total, count, ms);
        return resp;
    }

    @Transactional
    public List<AnalyticsCategoryBreakdownResponse> getCategoryBreakdown(String period, String paymentMethod, String account, String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics.svc] -> getCategoryBreakdown txId={} period={} paymentMethod={} account={}", transactionId, period, paymentMethod, account);
        List<Object[]> rows = budgetTransactionRepository.getCategoryBreakdown(period, blankToNull(paymentMethod), blankToNull(account));
        List<AnalyticsCategoryBreakdownResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new AnalyticsCategoryBreakdownResponse((String) r[0], (BigDecimal) r[1], (Long) r[2]));
        }
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics.svc] <- getCategoryBreakdown txId={} period={} rows={} durationMs={}", transactionId, period, out.size(), ms);
        return out;
    }

    @Transactional
    public List<AnalyticsCategoryBreakdownResponse> getTopCategories(String period, int limit, String paymentMethod, String account, String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics.svc] -> getTopCategories txId={} period={} limit={} paymentMethod={} account={}", transactionId, period, limit, paymentMethod, account);
        List<AnalyticsCategoryBreakdownResponse> all = getCategoryBreakdown(period, paymentMethod, account, transactionId);
        if (limit <= 0) return List.of();
        List<AnalyticsCategoryBreakdownResponse> out = all.subList(0, Math.min(limit, all.size()));
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics.svc] <- getTopCategories txId={} period={} rows={} durationMs={}", transactionId, period, out.size(), ms);
        return out;
    }

    @Transactional
    public List<AnalyticsAccountBreakdownResponse> getAccountBreakdown(String period, String paymentMethod, String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics.svc] -> getAccountBreakdown txId={} period={} paymentMethod={}", transactionId, period, paymentMethod);
        // Use repository two-arg overload (no account filter) for compatibility.
        List<Object[]> rows = budgetTransactionRepository.getAccountBreakdown(period, blankToNull(paymentMethod));
        List<AnalyticsAccountBreakdownResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new AnalyticsAccountBreakdownResponse((String) r[0], (BigDecimal) r[1], (Long) r[2]));
        }
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics.svc] <- getAccountBreakdown txId={} period={} rows={} durationMs={}", transactionId, period, out.size(), ms);
        return out;
    }

    @Transactional
    public List<AnalyticsPaymentMethodBreakdownResponse> getPaymentMethodBreakdown(String period, String account, String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics.svc] -> getPaymentMethodBreakdown txId={} period={} account={}", transactionId, period, account);
        List<Object[]> rows = budgetTransactionRepository.getPaymentMethodBreakdown(period, blankToNull(account));
        List<AnalyticsPaymentMethodBreakdownResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new AnalyticsPaymentMethodBreakdownResponse((String) r[0], (BigDecimal) r[1], (Long) r[2]));
        }
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics.svc] <- getPaymentMethodBreakdown txId={} period={} rows={} durationMs={}", transactionId, period, out.size(), ms);
        return out;
    }

    @Transactional
    public List<AnalyticsDailyTotalResponse> getDailyTotals(String period, String paymentMethod, String account, String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics.svc] -> getDailyTotals txId={} period={} paymentMethod={} account={}", transactionId, period, paymentMethod, account);
        List<Object[]> rows = budgetTransactionRepository.getDailyTotals(period, blankToNull(paymentMethod), blankToNull(account));
        List<AnalyticsDailyTotalResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new AnalyticsDailyTotalResponse((LocalDate) r[0], (BigDecimal) r[1], (Long) r[2]));
        }
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics.svc] <- getDailyTotals txId={} period={} rows={} durationMs={}", transactionId, period, out.size(), ms);
        return out;
    }

    @Transactional
    public List<AnalyticsCriticalityBreakdownResponse> getCriticalityBreakdown(String period, String paymentMethod, String account, String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics.svc] -> getCriticalityBreakdown txId={} period={} paymentMethod={} account={}", transactionId, period, paymentMethod, account);
        List<Object[]> rows = budgetTransactionRepository.getCriticalityBreakdown(period, blankToNull(paymentMethod), blankToNull(account));
        List<AnalyticsCriticalityBreakdownResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new AnalyticsCriticalityBreakdownResponse((String) r[0], (BigDecimal) r[1], (Long) r[2]));
        }
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics.svc] <- getCriticalityBreakdown txId={} period={} rows={} durationMs={}", transactionId, period, out.size(), ms);
        return out;
    }

    @Transactional
    public List<AnalyticsDuplicateResponse> getDuplicates(String period, String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics.svc] -> getDuplicates txId={} period={}", transactionId, period);
        List<Object[]> rows = budgetTransactionRepository.findDuplicatesByRowHash(period);
        List<AnalyticsDuplicateResponse> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new AnalyticsDuplicateResponse((String) r[0], (Long) r[1], (BigDecimal) r[2]));
        }
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics.svc] <- getDuplicates txId={} period={} rows={} durationMs={}", transactionId, period, out.size(), ms);
        return out;
    }

    @Transactional
    public List<BudgetTransaction> getUncategorized(String period, String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics.svc] -> getUncategorized txId={} period={}", transactionId, period);
        List<BudgetTransaction> out = budgetTransactionRepository.findUncategorized(period);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics.svc] <- getUncategorized txId={} period={} rows={} durationMs={}", transactionId, period, out.size(), ms);
        return out;
    }

    @Transactional
    public List<BudgetTransaction> getOutliers(String period, int limit, String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics.svc] -> getOutliers txId={} period={} limit={}", transactionId, period, limit);
        int safeLimit = Math.max(0, Math.min(limit, 500));
        if (safeLimit == 0) return List.of();
        List<BudgetTransaction> out = budgetTransactionRepository.findTopByStatementPeriodOrderByAmountDesc(period, PageRequest.of(0, safeLimit));
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics.svc] <- getOutliers txId={} period={} limit={} rows={} durationMs={}", transactionId, period, safeLimit, out.size(), ms);
        return out;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // Helper: convert various DB-returned objects into BigDecimal safely.
    private static BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        try {
            if (obj instanceof BigDecimal) return (BigDecimal) obj;
            if (obj instanceof java.math.BigInteger) return new BigDecimal((java.math.BigInteger) obj);
            if (obj instanceof Number) {
                // safe generic conversion (may lose precision for very large BigInteger -> double)
                return BigDecimal.valueOf(((Number) obj).doubleValue());
            }
            String s;
            if (obj instanceof String) {
                s = (String) obj;
            } else if (obj instanceof char[]) {
                s = new String((char[]) obj);
            } else if (obj instanceof byte[]) {
                s = new String((byte[]) obj);
            } else {
                s = obj.toString();
            }
            if (s == null) return BigDecimal.ZERO;
            // Remove grouping commas and common currency symbols/whitespace but keep exponent/e-signs
            String cleaned = s.trim().replaceAll("[,$£€\u00A0]", "");
            // Remove any characters except digits, dot, sign and exponent markers
            cleaned = cleaned.replaceAll("[^0-9eE+\\-.]", "");
            if (cleaned.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(cleaned);
        } catch (Exception ex) {
            // Fallback: try a double parse, log the original object for debugging
            try {
                double d = Double.parseDouble(obj.toString().replaceAll("[^0-9eE+\\-.]", ""));
                return BigDecimal.valueOf(d);
            } catch (Exception ex2) {
                // give up and return zero, but log original for investigation
                LoggerFactory.getLogger(AnalyticsService.class).warn("toBigDecimal failed to parse {}", obj, ex2);
                return BigDecimal.ZERO;
            }
        }
    }

    // Helper: convert various DB-returned objects into Long safely.
    private static Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        String s;
        if (obj instanceof String) s = (String) obj;
        else if (obj instanceof char[]) s = new String((char[]) obj);
        else if (obj instanceof byte[]) s = new String((byte[]) obj);
        else s = obj.toString();
        if (s == null) return 0L;
        // Strip decimals and non-digits
        s = s.trim().replaceAll("[^0-9\\-+ ]", "");
        if (s.isEmpty()) return 0L;
        try {
            if (s.contains(".")) {
                // parse as double then cast
                return (long) Double.parseDouble(s);
            }
            return Long.parseLong(s.trim());
        } catch (Exception ex) {
            try {
                double d = Double.parseDouble(s);
                return (long) d;
            } catch (Exception ex2) {
                LoggerFactory.getLogger(AnalyticsService.class).warn("toLong failed to parse {}", obj, ex2);
                return 0L;
            }
        }
    }
}
