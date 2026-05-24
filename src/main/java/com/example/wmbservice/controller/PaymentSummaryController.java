package com.example.wmbservice.controller;

import com.example.wmbservice.model.PaymentSummaryResponse;
import com.example.wmbservice.service.PaymentSummaryService;
import com.example.wmbservice.service.ProjectedTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment-summary")
public class PaymentSummaryController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentSummaryController.class);
    private final ProjectedTransactionService projectedTransactionService;
    private final PaymentSummaryService paymentSummaryService;

    public PaymentSummaryController(ProjectedTransactionService projectedTransactionService, PaymentSummaryService paymentSummaryService) {
        this.projectedTransactionService = projectedTransactionService;
        this.paymentSummaryService = paymentSummaryService;
    }

    /**
     * Returns a summary for each account, showing how much is owed on each credit card.
     * Accepts a comma-separated list of accounts.
     */
    @GetMapping
    public ResponseEntity<List<PaymentSummaryResponse>> getPaymentSummary(
            @RequestParam(value = "accounts") String accounts,
            @RequestParam(value = "statementPeriod") String statementPeriod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        logger.info("getPaymentSummary entered. transactionId={}, statementPeriod={}, accounts={}", transactionId, statementPeriod, accounts);
        if (statementPeriod == null || statementPeriod.isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        List<String> accountList = Arrays.stream(accounts.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        List<PaymentSummaryResponse> summaries = paymentSummaryService.getPaymentSummary(accountList, statementPeriod, transactionId);
        return ResponseEntity.ok(summaries);
    }

    /**
     * Overload: payment summary by inclusive transactionDate range.
     */
    @GetMapping(params = {"accounts", "startDate", "endDate"})
    public ResponseEntity<List<PaymentSummaryResponse>> getPaymentSummaryByDateRange(
            @RequestParam(value = "accounts") String accounts,
            @RequestParam(value = "startDate") String startDate,
            @RequestParam(value = "endDate") String endDate,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        logger.info("getPaymentSummaryByDateRange entered. transactionId={}, startDate={}, endDate={}, accounts={}",
                transactionId, startDate, endDate, accounts);

        try {
            LocalDate s = LocalDate.parse(startDate.trim());
            LocalDate e = LocalDate.parse(endDate.trim());

            List<String> accountList = Arrays.stream(accounts.split(","))
                    .map(String::trim)
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());

            List<PaymentSummaryResponse> summaries = paymentSummaryService.getPaymentSummaryByDateRange(accountList, s, e, transactionId);
            return ResponseEntity.ok(summaries);
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
