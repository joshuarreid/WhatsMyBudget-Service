package com.example.wmbservice.controller;

import com.example.wmbservice.model.PaymentSummaryResponse;
import com.example.wmbservice.service.PaymentSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * V2 API (JWT protected): Payment summary endpoints.
 * Mirrors v1 behavior but lives under /api/v2/payment-summary.
 */
@RestController
@RequestMapping("/api/v2/payment-summary")
public class PaymentSummaryControllerV2 {
    private static final Logger logger = LoggerFactory.getLogger(PaymentSummaryControllerV2.class);
    private final PaymentSummaryService paymentSummaryService;

    public PaymentSummaryControllerV2(PaymentSummaryService paymentSummaryService) {
        this.paymentSummaryService = paymentSummaryService;
    }

    private static String ensureTransactionId(String transactionId) {
        if (transactionId == null || transactionId.isBlank() || "N/A".equalsIgnoreCase(transactionId)) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return transactionId;
    }

    @GetMapping
    public ResponseEntity<List<PaymentSummaryResponse>> getPaymentSummary(
            @RequestParam(value = "accounts") String accounts,
            @RequestParam(value = "statementPeriod") String statementPeriod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        logger.info("[v2] getPaymentSummary entered. transactionId={}, statementPeriod={}, accounts={}", transactionId, statementPeriod, accounts);
        if (statementPeriod == null || statementPeriod.isBlank()) {
            return ResponseEntity.badRequest()
                    .header("X-Transaction-ID", transactionId)
                    .body(null);
        }
        List<String> accountList = Arrays.stream(accounts.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        List<PaymentSummaryResponse> summaries = paymentSummaryService.getPaymentSummary(accountList, statementPeriod, transactionId);
        return ResponseEntity.ok()
                .header("X-Transaction-ID", transactionId)
                .body(summaries);
    }

    @GetMapping(params = {"accounts", "startDate", "endDate"})
    public ResponseEntity<List<PaymentSummaryResponse>> getPaymentSummaryByDateRange(
            @RequestParam(value = "accounts") String accounts,
            @RequestParam(value = "startDate") String startDate,
            @RequestParam(value = "endDate") String endDate,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        transactionId = ensureTransactionId(transactionId);
        logger.info("[v2] getPaymentSummaryByDateRange entered. transactionId={}, startDate={}, endDate={}, accounts={}",
                transactionId, startDate, endDate, accounts);

        try {
            LocalDate s = LocalDate.parse(startDate.trim());
            LocalDate e = LocalDate.parse(endDate.trim());

            List<String> accountList = Arrays.stream(accounts.split(","))
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());

            List<PaymentSummaryResponse> summaries = paymentSummaryService.getPaymentSummaryByDateRange(accountList, s, e, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(summaries);
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .header("X-Transaction-ID", transactionId)
                    .body(null);
        }
    }
}
