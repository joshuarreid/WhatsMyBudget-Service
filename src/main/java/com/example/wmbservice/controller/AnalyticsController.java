package com.example.wmbservice.controller;

import com.example.wmbservice.dto.*;
import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Analytics endpoints for budget transaction insights.
 * All endpoints return analytics on actual (not projected) data.
 *
 * All endpoints accept and propagate X-Transaction-ID for traceability.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * List all available statement periods.
     */
    @GetMapping("/periods")
    public ResponseEntity<AnalyticsPeriodsResponse> getAllPeriods(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        AnalyticsPeriodsResponse result = analyticsService.getAllPeriods(transactionId);
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
        if (period == null || period.isBlank()) {
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        AnalyticsPeriodOverviewResponse result = analyticsService.getPeriodOverview(period, paymentMethod, account, transactionId);
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
        if (period == null || period.isBlank()) {
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsCategoryBreakdownResponse> result = analyticsService.getCategoryBreakdown(period, paymentMethod, account, transactionId);
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
        if (period == null || period.isBlank()) {
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        int safeLimit = Math.max(0, Math.min(limit, 100));
        List<AnalyticsCategoryBreakdownResponse> result = analyticsService.getTopCategories(period, safeLimit, paymentMethod, account, transactionId);
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
        if (period == null || period.isBlank()) {
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsAccountBreakdownResponse> result = analyticsService.getAccountBreakdown(period, paymentMethod, transactionId);
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
        if (period == null || period.isBlank()) {
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsPaymentMethodBreakdownResponse> result = analyticsService.getPaymentMethodBreakdown(period, account, transactionId);
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
        if (period == null || period.isBlank()) {
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsDailyTotalResponse> result = analyticsService.getDailyTotals(period, paymentMethod, account, transactionId);
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
        if (period == null || period.isBlank()) {
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsCriticalityBreakdownResponse> result = analyticsService.getCriticalityBreakdown(period, paymentMethod, account, transactionId);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    /**
     * Find duplicate transactions by row_hash for a period.
     */
    @GetMapping("/periods/{period}/duplicates")
    public ResponseEntity<List<AnalyticsDuplicateResponse>> getDuplicates(
            @PathVariable String period,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        if (period == null || period.isBlank()) {
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<AnalyticsDuplicateResponse> result = analyticsService.getDuplicates(period, transactionId);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }

    /**
     * Find uncategorized transactions for a period.
     */
    @GetMapping("/periods/{period}/uncategorized")
    public ResponseEntity<List<BudgetTransaction>> getUncategorized(
            @PathVariable String period,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        if (period == null || period.isBlank()) {
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        List<BudgetTransaction> result = analyticsService.getUncategorized(period, transactionId);
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
        if (period == null || period.isBlank()) {
            return ResponseEntity.badRequest().header("X-Transaction-ID", transactionId).build();
        }
        int safeLimit = Math.max(0, Math.min(limit, 200));
        List<BudgetTransaction> result = analyticsService.getOutliers(period, safeLimit, transactionId);
        return ResponseEntity.ok().header("X-Transaction-ID", transactionId).body(result);
    }
}
