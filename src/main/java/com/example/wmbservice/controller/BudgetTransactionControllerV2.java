package com.example.wmbservice.controller;

import com.example.wmbservice.model.AccountBudgetTransactionList;
import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.model.BudgetTransactionList;
import com.example.wmbservice.service.BudgetTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * V2 API (OAuth2 protected): read-only endpoints for BudgetTransaction.
 */
@CrossOrigin(origins = "http://localhost:3000", exposedHeaders = "X-Transaction-ID")
@RestController
@RequestMapping("/api/v2/transactions")
public class BudgetTransactionControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(BudgetTransactionControllerV2.class);

    private final BudgetTransactionService budgetTransactionService;

    public BudgetTransactionControllerV2(BudgetTransactionService budgetTransactionService) {
        this.budgetTransactionService = budgetTransactionService;
    }

    @GetMapping
    public ResponseEntity<?> getTransactions(
            @RequestParam(value = "statementPeriod", required = false) String statementPeriod,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "criticality_id", required = false) Long criticalityId,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] getTransactions entered. transactionId={}, filters: statementPeriod={}, account={}, category={}, paymentMethod={}",
                transactionId, statementPeriod, account, category, paymentMethod);

        try {
            BudgetTransactionList result = budgetTransactionService.getTransactions(
                    statementPeriod, account, category, criticality, criticalityId, paymentMethod, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("[v2] Error fetching transactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "LIST_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Overload: list transactions by inclusive transactionDate range.
     */
    @GetMapping(params = {"startDate", "endDate"})
    public ResponseEntity<?> getTransactionsByDateRange(
            @RequestParam(value = "startDate") String startDate,
            @RequestParam(value = "endDate") String endDate,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "criticality_id", required = false) Long criticalityId,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] getTransactionsByDateRange entered. transactionId={}, startDate={}, endDate={}, account={}, category={}, paymentMethod={}",
                transactionId, startDate, endDate, account, category, paymentMethod);

        try {
            LocalDate s = LocalDate.parse(startDate.trim());
            LocalDate e = LocalDate.parse(endDate.trim());
            BudgetTransactionList result = budgetTransactionService.getTransactionsByDateRange(
                    s, e, account, category, criticality, criticalityId, paymentMethod, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            logger.warn("[v2] Invalid date range in getTransactionsByDateRange. transactionId={}, startDate={}, endDate={}, error={}",
                    transactionId, startDate, endDate, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error fetching transactions by date range. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "LIST_ERROR", "Unexpected error", transactionId));
        }
    }

    @GetMapping("/account")
    public ResponseEntity<?> getTransactionsForAccount(
            @RequestParam("account") String account,
            @RequestParam(value = "statementPeriod", required = false) String statementPeriod,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "criticality_id", required = false) Long criticalityId,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId
    ) {
        logger.info("[v2] getTransactionsForAccount entered. transactionId={}, account={}", transactionId, account);

        try {
            AccountBudgetTransactionList result = budgetTransactionService.getAccountBudgetTransactionList(
                    account, statementPeriod, category, criticality, criticalityId, paymentMethod, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("[v2] Error in getTransactionsForAccount. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ACCOUNT_TX_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Overload: account view by inclusive transactionDate range.
     */
    @GetMapping(value = "/account", params = {"account", "startDate", "endDate"})
    public ResponseEntity<?> getTransactionsForAccountByDateRange(
            @RequestParam("account") String account,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "criticality_id", required = false) Long criticalityId,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId
    ) {
        logger.info("[v2] getTransactionsForAccountByDateRange entered. transactionId={}, account={}, startDate={}, endDate={}",
                transactionId, account, startDate, endDate);
        try {
            LocalDate s = LocalDate.parse(startDate.trim());
            LocalDate e = LocalDate.parse(endDate.trim());
            AccountBudgetTransactionList result = budgetTransactionService.getAccountBudgetTransactionListByDateRange(
                    account, s, e, category, criticality, criticalityId, paymentMethod, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            logger.warn("[v2] Invalid date range in getTransactionsForAccountByDateRange. transactionId={}, account={}, startDate={}, endDate={}, error={}",
                    transactionId, account, startDate, endDate, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error in getTransactionsForAccountByDateRange. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ACCOUNT_TX_ERROR", "Unexpected error", transactionId));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] getTransaction entered. transactionId={}, id={}", transactionId, id);

        try {
            BudgetTransaction transaction = budgetTransactionService.getTransaction(id, transactionId);
            if (transaction == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Transaction-ID", transactionId)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Transaction not found", transactionId));
            }
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(transaction);
        } catch (Exception e) {
            logger.error("[v2] Error getting transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "GET_ERROR", "Unexpected error", transactionId));
        }
    }

    private static class ErrorResponse {
        public final int status;
        public final String code;
        public final String message;
        public final String transactionId;

        public ErrorResponse(int status, String code, String message, String transactionId) {
            this.status = status;
            this.code = code;
            this.message = message;
            this.transactionId = transactionId;
        }
    }
}
