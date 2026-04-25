package com.example.wmbservice.controller;

import com.example.wmbservice.model.AccountBudgetTransactionList;
import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.model.BudgetTransactionList;
import com.example.wmbservice.service.BudgetTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] getTransactions entered. transactionId={}, filters: statementPeriod={}, account={}, category={}, paymentMethod={}",
                transactionId, statementPeriod, account, category, paymentMethod);

        try {
            BudgetTransactionList result = budgetTransactionService.getTransactions(
                    statementPeriod, account, category, criticality, paymentMethod, transactionId);
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

    @GetMapping("/account")
    public ResponseEntity<?> getTransactionsForAccount(
            @RequestParam("account") String account,
            @RequestParam(value = "statementPeriod", required = false) String statementPeriod,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId
    ) {
        logger.info("[v2] getTransactionsForAccount entered. transactionId={}, account={}", transactionId, account);

        try {
            AccountBudgetTransactionList result = budgetTransactionService.getAccountBudgetTransactionList(
                    account, statementPeriod, category, criticality, paymentMethod, transactionId);
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

