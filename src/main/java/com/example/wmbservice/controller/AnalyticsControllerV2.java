package com.example.wmbservice.controller;

import com.example.wmbservice.dto.*;
import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.service.AnalyticsService;
import com.example.wmbservice.service.StatementPeriodSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * V2 API (JWT protected): Analytics endpoints for budget transaction insights.
 * Mirrors v1 behavior but lives under /api/v2/analytics.
 */
@RestController
@RequestMapping("/api/v2/analytics")
public class AnalyticsControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsControllerV2.class);

    private final AnalyticsService analyticsService;
    private final StatementPeriodSummaryService statementPeriodSummaryService;

    public AnalyticsControllerV2(AnalyticsService analyticsService,
                                  StatementPeriodSummaryService statementPeriodSummaryService) {
        this.analyticsService = analyticsService;
        this.statementPeriodSummaryService = statementPeriodSummaryService;
    }

    private static String ensureTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank() || "N/A".equalsIgnoreCase(transactionId)) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return transactionId;
    }

    private static LocalDate parseIsoDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value.trim());
    }

    private static boolean isInvalidRange(LocalDate start, LocalDate end) {
        return start == null || end == null || start.isAfter(end);
    }

    @GetMapping("/periods")
    public ResponseEntity<AnalyticsPeriodsResponse> getAllPeriods(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /periods txId={}", transactionId);
        AnalyticsPeriodsResponse result = analyticsService.getAllPeriods(transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        Integer count = result != null ? result.getCount() : null;
        logger.info("[v2][analytics] <- GET /periods txId={} status=200 periods={} durationMs={}", transactionId, count, ms);
        return ResponseEntity.ok()
                .header("X-Transaction-ID", transactionId)
                .body(result);
    }

    @GetMapping("/periods/{period}/overview")
    public ResponseEntity<AnalyticsPeriodOverviewResponse> getPeriodOverview(
            @PathVariable String period,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /periods/{}/overview txId={} paymentMethod={} account={}", period, transactionId, paymentMethod, account);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /periods/<blank>/overview txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        AnalyticsPeriodOverviewResponse result = analyticsService.getPeriodOverview(period, paymentMethod, account, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[v2][analytics] <- GET /periods/{}/overview txId={} status=200 total={} count={} durationMs={}",
                period, transactionId,
                result != null ? result.getTotalAmount() : null,
                result != null ? result.getTransactionCount() : null,
                ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    @GetMapping("/range/overview")
    public ResponseEntity<AnalyticsPeriodOverviewResponse> getDateRangeOverview(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /range/overview txId={} startDate={} endDate={} paymentMethod={} account={}",
                transactionId, startDate, endDate, paymentMethod, account);
        try {
            LocalDate s = parseIsoDate(startDate);
            LocalDate e = parseIsoDate(endDate);
            if (isInvalidRange(s, e)) {
                long ms = (System.nanoTime() - startNs) / 1_000_000;
                logger.warn("[v2][analytics] <- GET /range/overview txId={} status=400 durationMs={}", transactionId, ms);
                return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
            }
            AnalyticsPeriodOverviewResponse result = analyticsService.getDateRangeOverview(s, e, paymentMethod, account, transactionId);
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("[v2][analytics] <- GET /range/overview txId={} status=200 total={} count={} durationMs={}",
                    transactionId,
                    result != null ? result.getTotalAmount() : null,
                    result != null ? result.getTransactionCount() : null,
                    ms);
            return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
        } catch (DateTimeParseException ex) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /range/overview txId={} status=400 badDate durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
    }

    @GetMapping("/periods/{period}/categories")
    public ResponseEntity<List<AnalyticsCategoryBreakdownResponse>> getCategoryBreakdown(
            @PathVariable String period,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /periods/{}/categories txId={} paymentMethod={} account={}", period, transactionId, paymentMethod, account);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /periods/<blank>/categories txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsCategoryBreakdownResponse> result = analyticsService.getCategoryBreakdown(period, paymentMethod, account, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[v2][analytics] <- GET /periods/{}/categories txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    @GetMapping("/range/categories")
    public ResponseEntity<List<AnalyticsCategoryBreakdownResponse>> getCategoryBreakdownByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /range/categories txId={} startDate={} endDate={} paymentMethod={} account={}",
                transactionId, startDate, endDate, paymentMethod, account);
        try {
            LocalDate s = parseIsoDate(startDate);
            LocalDate e = parseIsoDate(endDate);
            if (isInvalidRange(s, e)) {
                long ms = (System.nanoTime() - startNs) / 1_000_000;
                logger.warn("[v2][analytics] <- GET /range/categories txId={} status=400 durationMs={}", transactionId, ms);
                return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
            }
            List<AnalyticsCategoryBreakdownResponse> result = analyticsService.getCategoryBreakdownByDateRange(s, e, paymentMethod, account, transactionId);
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("[v2][analytics] <- GET /range/categories txId={} status=200 rows={} durationMs={}", transactionId, result != null ? result.size() : null, ms);
            return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
        } catch (DateTimeParseException ex) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /range/categories txId={} status=400 badDate durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
    }

    @GetMapping("/categories/distinct")
    public ResponseEntity<List<String>> getDistinctCategories(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /categories/distinct txId={}", transactionId);
        List<String> result = analyticsService.getDistinctCategories(transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[v2][analytics] <- GET /categories/distinct txId={} status=200 rows={} durationMs={}",
                transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    @GetMapping("/periods/{period}/categories/top")
    public ResponseEntity<List<AnalyticsCategoryBreakdownResponse>> getTopCategories(
            @PathVariable String period,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /periods/{}/categories/top txId={} limit={} paymentMethod={} account={}", period, transactionId, limit, paymentMethod, account);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /periods/<blank>/categories/top txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        int safeLimit = Math.max(0, Math.min(limit, 100));
        List<AnalyticsCategoryBreakdownResponse> result = analyticsService.getTopCategories(period, safeLimit, paymentMethod, account, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[v2][analytics] <- GET /periods/{}/categories/top txId={} status=200 limit={} rows={} durationMs={}",
                period, transactionId, safeLimit, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    @GetMapping("/periods/{period}/categories/distinct")
    public ResponseEntity<List<String>> getDistinctCategoriesByPeriod(
            @PathVariable String period,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /periods/{}/categories/distinct txId={}", period, transactionId);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /periods/<blank>/categories/distinct txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<String> result = analyticsService.getDistinctCategories(period, null, null, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[v2][analytics] <- GET /periods/{}/categories/distinct txId={} status=200 rows={} durationMs={}",
                period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    @GetMapping("/range/categories/top")
    public ResponseEntity<List<AnalyticsCategoryBreakdownResponse>> getTopCategoriesByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /range/categories/top txId={} startDate={} endDate={} limit={} paymentMethod={} account={}",
                transactionId, startDate, endDate, limit, paymentMethod, account);
        try {
            LocalDate s = parseIsoDate(startDate);
            LocalDate e = parseIsoDate(endDate);
            if (isInvalidRange(s, e)) {
                long ms = (System.nanoTime() - startNs) / 1_000_000;
                logger.warn("[v2][analytics] <- GET /range/categories/top txId={} status=400 durationMs={}", transactionId, ms);
                return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
            }
            int safeLimit = Math.max(0, Math.min(limit, 100));
            List<AnalyticsCategoryBreakdownResponse> result = analyticsService.getTopCategoriesByDateRange(s, e, safeLimit, paymentMethod, account, transactionId);
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("[v2][analytics] <- GET /range/categories/top txId={} status=200 limit={} rows={} durationMs={}",
                    transactionId, safeLimit, result != null ? result.size() : null, ms);
            return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
        } catch (DateTimeParseException ex) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /range/categories/top txId={} status=400 badDate durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
    }

    @GetMapping("/periods/{period}/accounts")
    public ResponseEntity<List<AnalyticsAccountBreakdownResponse>> getAccountBreakdown(
            @PathVariable String period,
            @RequestParam(required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /periods/{}/accounts txId={} paymentMethod={}", period, transactionId, paymentMethod);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /periods/<blank>/accounts txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsAccountBreakdownResponse> result = analyticsService.getAccountBreakdown(period, paymentMethod, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[v2][analytics] <- GET /periods/{}/accounts txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    @GetMapping("/range/accounts")
    public ResponseEntity<List<AnalyticsAccountBreakdownResponse>> getAccountBreakdownByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /range/accounts txId={} startDate={} endDate={} paymentMethod={}",
                transactionId, startDate, endDate, paymentMethod);
        try {
            LocalDate s = parseIsoDate(startDate);
            LocalDate e = parseIsoDate(endDate);
            if (isInvalidRange(s, e)) {
                long ms = (System.nanoTime() - startNs) / 1_000_000;
                logger.warn("[v2][analytics] <- GET /range/accounts txId={} status=400 durationMs={}", transactionId, ms);
                return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
            }
            List<AnalyticsAccountBreakdownResponse> result = analyticsService.getAccountBreakdownByDateRange(s, e, paymentMethod, transactionId);
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("[v2][analytics] <- GET /range/accounts txId={} status=200 rows={} durationMs={}", transactionId, result != null ? result.size() : null, ms);
            return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
        } catch (DateTimeParseException ex) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /range/accounts txId={} status=400 badDate durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
    }

    @GetMapping("/periods/{period}/payment-methods")
    public ResponseEntity<List<AnalyticsPaymentMethodBreakdownResponse>> getPaymentMethodBreakdown(
            @PathVariable String period,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /periods/{}/payment-methods txId={} account={}", period, transactionId, account);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /periods/<blank>/payment-methods txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsPaymentMethodBreakdownResponse> result = analyticsService.getPaymentMethodBreakdown(period, account, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[v2][analytics] <- GET /periods/{}/payment-methods txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    @GetMapping("/range/payment-methods")
    public ResponseEntity<List<AnalyticsPaymentMethodBreakdownResponse>> getPaymentMethodBreakdownByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /range/payment-methods txId={} startDate={} endDate={} account={}",
                transactionId, startDate, endDate, account);
        try {
            LocalDate s = parseIsoDate(startDate);
            LocalDate e = parseIsoDate(endDate);
            if (isInvalidRange(s, e)) {
                long ms = (System.nanoTime() - startNs) / 1_000_000;
                logger.warn("[v2][analytics] <- GET /range/payment-methods txId={} status=400 durationMs={}", transactionId, ms);
                return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
            }
            List<AnalyticsPaymentMethodBreakdownResponse> result = analyticsService.getPaymentMethodBreakdownByDateRange(s, e, account, transactionId);
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("[v2][analytics] <- GET /range/payment-methods txId={} status=200 rows={} durationMs={}", transactionId, result != null ? result.size() : null, ms);
            return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
        } catch (DateTimeParseException ex) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /range/payment-methods txId={} status=400 badDate durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
    }

    @GetMapping("/periods/{period}/daily")
    public ResponseEntity<List<AnalyticsDailyTotalResponse>> getDailyTotals(
            @PathVariable String period,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /periods/{}/daily txId={} paymentMethod={} account={}", period, transactionId, paymentMethod, account);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /periods/<blank>/daily txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsDailyTotalResponse> result = analyticsService.getDailyTotals(period, paymentMethod, account, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[v2][analytics] <- GET /periods/{}/daily txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    @GetMapping("/range/daily")
    public ResponseEntity<List<AnalyticsDailyTotalResponse>> getDailyTotalsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /range/daily txId={} startDate={} endDate={} paymentMethod={} account={}",
                transactionId, startDate, endDate, paymentMethod, account);
        try {
            LocalDate s = parseIsoDate(startDate);
            LocalDate e = parseIsoDate(endDate);
            if (isInvalidRange(s, e)) {
                long ms = (System.nanoTime() - startNs) / 1_000_000;
                logger.warn("[v2][analytics] <- GET /range/daily txId={} status=400 durationMs={}", transactionId, ms);
                return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
            }
            List<AnalyticsDailyTotalResponse> result = analyticsService.getDailyTotalsByDateRange(s, e, paymentMethod, account, transactionId);
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("[v2][analytics] <- GET /range/daily txId={} status=200 rows={} durationMs={}", transactionId, result != null ? result.size() : null, ms);
            return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
        } catch (DateTimeParseException ex) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /range/daily txId={} status=400 badDate durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
    }

    @GetMapping("/periods/{period}/criticality")
    public ResponseEntity<List<AnalyticsCriticalityBreakdownResponse>> getCriticalityBreakdown(
            @PathVariable String period,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /periods/{}/criticality txId={} paymentMethod={} account={}", period, transactionId, paymentMethod, account);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /periods/<blank>/criticality txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsCriticalityBreakdownResponse> result = analyticsService.getCriticalityBreakdown(period, paymentMethod, account, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[v2][analytics] <- GET /periods/{}/criticality txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    @GetMapping("/range/criticality")
    public ResponseEntity<List<AnalyticsCriticalityBreakdownResponse>> getCriticalityBreakdownByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /range/criticality txId={} startDate={} endDate={} paymentMethod={} account={}",
                transactionId, startDate, endDate, paymentMethod, account);
        try {
            LocalDate s = parseIsoDate(startDate);
            LocalDate e = parseIsoDate(endDate);
            if (isInvalidRange(s, e)) {
                long ms = (System.nanoTime() - startNs) / 1_000_000;
                logger.warn("[v2][analytics] <- GET /range/criticality txId={} status=400 durationMs={}", transactionId, ms);
                return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
            }
            List<AnalyticsCriticalityBreakdownResponse> result = analyticsService.getCriticalityBreakdownByDateRange(s, e, paymentMethod, account, transactionId);
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("[v2][analytics] <- GET /range/criticality txId={} status=200 rows={} durationMs={}", transactionId, result != null ? result.size() : null, ms);
            return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
        } catch (DateTimeParseException ex) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /range/criticality txId={} status=400 badDate durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
    }

    @GetMapping("/periods/{period}/duplicates")
    public ResponseEntity<List<AnalyticsDuplicateResponse>> getDuplicates(
            @PathVariable String period,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /periods/{}/duplicates txId={}", period, transactionId);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /periods/<blank>/duplicates txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsDuplicateResponse> result = analyticsService.getDuplicates(period, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[v2][analytics] <- GET /periods/{}/duplicates txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    @GetMapping("/range/duplicates")
    public ResponseEntity<List<AnalyticsDuplicateResponse>> getDuplicatesByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /range/duplicates txId={} startDate={} endDate={}", transactionId, startDate, endDate);
        try {
            LocalDate s = parseIsoDate(startDate);
            LocalDate e = parseIsoDate(endDate);
            if (isInvalidRange(s, e)) {
                long ms = (System.nanoTime() - startNs) / 1_000_000;
                logger.warn("[v2][analytics] <- GET /range/duplicates txId={} status=400 durationMs={}", transactionId, ms);
                return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
            }
            List<AnalyticsDuplicateResponse> result = analyticsService.getDuplicatesByDateRange(s, e, transactionId);
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("[v2][analytics] <- GET /range/duplicates txId={} status=200 rows={} durationMs={}", transactionId, result != null ? result.size() : null, ms);
            return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
        } catch (DateTimeParseException ex) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /range/duplicates txId={} status=400 badDate durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
    }

    @GetMapping("/periods/{period}/uncategorized")
    public ResponseEntity<List<BudgetTransaction>> getUncategorized(
            @PathVariable String period,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /periods/{}/uncategorized txId={}", period, transactionId);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /periods/<blank>/uncategorized txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<BudgetTransaction> result = analyticsService.getUncategorized(period, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[v2][analytics] <- GET /periods/{}/uncategorized txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    @GetMapping("/range/uncategorized")
    public ResponseEntity<List<BudgetTransaction>> getUncategorizedByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /range/uncategorized txId={} startDate={} endDate={}", transactionId, startDate, endDate);
        try {
            LocalDate s = parseIsoDate(startDate);
            LocalDate e = parseIsoDate(endDate);
            if (isInvalidRange(s, e)) {
                long ms = (System.nanoTime() - startNs) / 1_000_000;
                logger.warn("[v2][analytics] <- GET /range/uncategorized txId={} status=400 durationMs={}", transactionId, ms);
                return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
            }
            List<BudgetTransaction> result = analyticsService.getUncategorizedByDateRange(s, e, transactionId);
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("[v2][analytics] <- GET /range/uncategorized txId={} status=200 rows={} durationMs={}", transactionId, result != null ? result.size() : null, ms);
            return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
        } catch (DateTimeParseException ex) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /range/uncategorized txId={} status=400 badDate durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
    }

    @GetMapping("/periods/{period}/outliers")
    public ResponseEntity<List<BudgetTransaction>> getOutliers(
            @PathVariable String period,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /periods/{}/outliers txId={} limit={}", period, transactionId, limit);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /periods/<blank>/outliers txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        int safeLimit = Math.max(0, Math.min(limit, 200));
        List<BudgetTransaction> result = analyticsService.getOutliers(period, safeLimit, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[v2][analytics] <- GET /periods/{}/outliers txId={} status=200 limit={} rows={} durationMs={}",
                period, transactionId, safeLimit, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    @GetMapping("/range/outliers")
    public ResponseEntity<List<BudgetTransaction>> getOutliersByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /range/outliers txId={} startDate={} endDate={} limit={}", transactionId, startDate, endDate, limit);
        try {
            LocalDate s = parseIsoDate(startDate);
            LocalDate e = parseIsoDate(endDate);
            if (isInvalidRange(s, e)) {
                long ms = (System.nanoTime() - startNs) / 1_000_000;
                logger.warn("[v2][analytics] <- GET /range/outliers txId={} status=400 durationMs={}", transactionId, ms);
                return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
            }
            int safeLimit = Math.max(0, Math.min(limit, 200));
            List<BudgetTransaction> result = analyticsService.getOutliersByDateRange(s, e, safeLimit, transactionId);
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("[v2][analytics] <- GET /range/outliers txId={} status=200 limit={} rows={} durationMs={}",
                    transactionId, safeLimit, result != null ? result.size() : null, ms);
            return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
        } catch (DateTimeParseException ex) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /range/outliers txId={} status=400 badDate durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
    }

    @GetMapping("/summaries/{period}")
    public ResponseEntity<AnalyticsStatementPeriodSummaryResponse> getStatementPeriodSummary(
            @PathVariable String period,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /summaries/{} txId={}", period, transactionId);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /summaries/<blank> txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        try {
            AnalyticsStatementPeriodSummaryResponse result = statementPeriodSummaryService.getSummary(period, transactionId);
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("[v2][analytics] <- GET /summaries/{} txId={} status=200 generatedAt={} durationMs={}",
                    period, transactionId, result != null ? result.getGeneratedAt() : null, ms);
            return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
        } catch (IllegalArgumentException ex) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /summaries/{} txId={} status=400 error={} durationMs={}",
                    period, transactionId, ex.getMessage(), ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
    }

    @GetMapping(value = "/summaries", params = {"startPeriod", "endPeriod"})
    public ResponseEntity<List<AnalyticsStatementPeriodSummaryResponse>> getStatementPeriodSummariesByRange(
            @RequestParam String startPeriod,
            @RequestParam String endPeriod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[v2][analytics] -> GET /summaries txId={} startPeriod={} endPeriod={}", transactionId, startPeriod, endPeriod);
        try {
            List<AnalyticsStatementPeriodSummaryResponse> result =
                    statementPeriodSummaryService.getSummariesByPeriodRange(startPeriod, endPeriod, transactionId);
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.info("[v2][analytics] <- GET /summaries txId={} status=200 rows={} durationMs={}",
                    transactionId, result != null ? result.size() : null, ms);
            return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
        } catch (IllegalArgumentException ex) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[v2][analytics] <- GET /summaries txId={} status=400 error={} durationMs={}",
                    transactionId, ex.getMessage(), ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
    }
}
