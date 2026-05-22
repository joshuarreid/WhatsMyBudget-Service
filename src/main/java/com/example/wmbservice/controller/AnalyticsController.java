package com.example.wmbservice.controller;

import com.example.wmbservice.dto.*;
import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Analytics endpoints for budget transaction insights.
 * All endpoints return analytics on actual (not projected) data.
 *
 * All endpoints accept and propagate X-Transaction-ID for traceability.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    private static String ensureTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank() || "N/A".equalsIgnoreCase(transactionId)) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return transactionId;
    }

    /**
     * List all available statement periods.
     */
    @GetMapping("/periods")
    public ResponseEntity<AnalyticsPeriodsResponse> getAllPeriods(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics] -> GET /periods txId={}", transactionId);
        AnalyticsPeriodsResponse result = analyticsService.getAllPeriods(transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        Integer count = result != null ? result.getCount() : null;
        logger.info("[analytics] <- GET /periods txId={} status=200 periods={} durationMs={}", transactionId, count, ms);
        return ResponseEntity.ok()
                .header("X-Transaction-ID", transactionId)
                .body(result);
    }

    /**
     * Overview for a given period (total spend, transaction count).
     */
    @GetMapping("/periods/{period}/overview")
    public ResponseEntity<AnalyticsPeriodOverviewResponse> getPeriodOverview(
            @PathVariable String period,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics] -> GET /periods/{}/overview txId={} paymentMethod={} account={}", period, transactionId, paymentMethod, account);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[analytics] <- GET /periods/<blank>/overview txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        AnalyticsPeriodOverviewResponse result = analyticsService.getPeriodOverview(period, paymentMethod, account, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics] <- GET /periods/{}/overview txId={} status=200 total={} count={} durationMs={}",
                period,
                transactionId,
                result != null ? result.getTotalAmount() : null,
                result != null ? result.getTransactionCount() : null,
                ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    /**
     * Sum and count by category for a period.
     */
    @GetMapping("/periods/{period}/categories")
    public ResponseEntity<List<AnalyticsCategoryBreakdownResponse>> getCategoryBreakdown(
            @PathVariable String period,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics] -> GET /periods/{}/categories txId={} paymentMethod={} account={}", period, transactionId, paymentMethod, account);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[analytics] <- GET /periods/<blank>/categories txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsCategoryBreakdownResponse> result = analyticsService.getCategoryBreakdown(period, paymentMethod, account, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics] <- GET /periods/{}/categories txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    /**
     * Top N categories by spend for a period.
     */
    @GetMapping("/periods/{period}/categories/top")
    public ResponseEntity<List<AnalyticsCategoryBreakdownResponse>> getTopCategories(
            @PathVariable String period,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics] -> GET /periods/{}/categories/top txId={} limit={} paymentMethod={} account={}", period, transactionId, limit, paymentMethod, account);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[analytics] <- GET /periods/<blank>/categories/top txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        int safeLimit = Math.max(0, Math.min(limit, 100));
        List<AnalyticsCategoryBreakdownResponse> result = analyticsService.getTopCategories(period, safeLimit, paymentMethod, account, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics] <- GET /periods/{}/categories/top txId={} status=200 limit={} rows={} durationMs={}",
                period, transactionId, safeLimit, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    /**
     * Sum and count by account for a period.
     */
    @GetMapping("/periods/{period}/accounts")
    public ResponseEntity<List<AnalyticsAccountBreakdownResponse>> getAccountBreakdown(
            @PathVariable String period,
            @RequestParam(required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics] -> GET /periods/{}/accounts txId={} paymentMethod={}", period, transactionId, paymentMethod);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[analytics] <- GET /periods/<blank>/accounts txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsAccountBreakdownResponse> result = analyticsService.getAccountBreakdown(period, paymentMethod, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics] <- GET /periods/{}/accounts txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    /**
     * Sum and count by payment method for a period.
     */
    @GetMapping("/periods/{period}/payment-methods")
    public ResponseEntity<List<AnalyticsPaymentMethodBreakdownResponse>> getPaymentMethodBreakdown(
            @PathVariable String period,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics] -> GET /periods/{}/payment-methods txId={} account={}", period, transactionId, account);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[analytics] <- GET /periods/<blank>/payment-methods txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsPaymentMethodBreakdownResponse> result = analyticsService.getPaymentMethodBreakdown(period, account, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics] <- GET /periods/{}/payment-methods txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    /**
     * Daily totals for a period (time series).
     */
    @GetMapping("/periods/{period}/daily")
    public ResponseEntity<List<AnalyticsDailyTotalResponse>> getDailyTotals(
            @PathVariable String period,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics] -> GET /periods/{}/daily txId={} paymentMethod={} account={}", period, transactionId, paymentMethod, account);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[analytics] <- GET /periods/<blank>/daily txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsDailyTotalResponse> result = analyticsService.getDailyTotals(period, paymentMethod, account, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics] <- GET /periods/{}/daily txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    /**
     * Sum and count by criticality for a period.
     */
    @GetMapping("/periods/{period}/criticality")
    public ResponseEntity<List<AnalyticsCriticalityBreakdownResponse>> getCriticalityBreakdown(
            @PathVariable String period,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String account,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics] -> GET /periods/{}/criticality txId={} paymentMethod={} account={}", period, transactionId, paymentMethod, account);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[analytics] <- GET /periods/<blank>/criticality txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsCriticalityBreakdownResponse> result = analyticsService.getCriticalityBreakdown(period, paymentMethod, account, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics] <- GET /periods/{}/criticality txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    /**
     * Find duplicate transactions by row_hash for a period.
     */
    @GetMapping("/periods/{period}/duplicates")
    public ResponseEntity<List<AnalyticsDuplicateResponse>> getDuplicates(
            @PathVariable String period,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics] -> GET /periods/{}/duplicates txId={}", period, transactionId);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[analytics] <- GET /periods/<blank>/duplicates txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsDuplicateResponse> result = analyticsService.getDuplicates(period, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics] <- GET /periods/{}/duplicates txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    /**
     * Find uncategorized transactions for a period.
     */
    @GetMapping("/periods/{period}/uncategorized")
    public ResponseEntity<List<BudgetTransaction>> getUncategorized(
            @PathVariable String period,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics] -> GET /periods/{}/uncategorized txId={}", period, transactionId);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[analytics] <- GET /periods/<blank>/uncategorized txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<BudgetTransaction> result = analyticsService.getUncategorized(period, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics] <- GET /periods/{}/uncategorized txId={} status=200 rows={} durationMs={}", period, transactionId, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    /**
     * Find outlier (largest) transactions for a period.
     */
    @GetMapping("/periods/{period}/outliers")
    public ResponseEntity<List<BudgetTransaction>> getOutliers(
            @PathVariable String period,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        long startNs = System.nanoTime();
        logger.info("[analytics] -> GET /periods/{}/outliers txId={} limit={}", period, transactionId, limit);
        if (period == null || period.isBlank()) {
            long ms = (System.nanoTime() - startNs) / 1_000_000;
            logger.warn("[analytics] <- GET /periods/<blank>/outliers txId={} status=400 durationMs={}", transactionId, ms);
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        int safeLimit = Math.max(0, Math.min(limit, 200));
        List<BudgetTransaction> result = analyticsService.getOutliers(period, safeLimit, transactionId);
        long ms = (System.nanoTime() - startNs) / 1_000_000;
        logger.info("[analytics] <- GET /periods/{}/outliers txId={} status=200 limit={} rows={} durationMs={}",
                period, transactionId, safeLimit, result != null ? result.size() : null, ms);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }
}
