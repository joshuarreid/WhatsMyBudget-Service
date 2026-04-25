package com.example.wmbservice.controller;

import com.example.wmbservice.model.AccountProjectedTransactionList;
import com.example.wmbservice.model.ProjectedTransaction;
import com.example.wmbservice.model.ProjectedTransactionList;
import com.example.wmbservice.service.ProjectedTransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * V2 API (OAuth2 protected): full CRUD endpoints for ProjectedTransaction.
 */
@CrossOrigin(origins = "http://localhost:3000", exposedHeaders = "X-Transaction-ID")
@RestController
@RequestMapping("/api/v2/projected-transactions")
public class ProjectedTransactionControllerV2 {
    private static final Logger logger = LoggerFactory.getLogger(ProjectedTransactionControllerV2.class);
    private final ProjectedTransactionService projectedTransactionService;

    public ProjectedTransactionControllerV2(ProjectedTransactionService projectedTransactionService) {
        this.projectedTransactionService = projectedTransactionService;
    }

    @PostMapping
    public ResponseEntity<?> createTransaction(
            @Valid @RequestBody ProjectedTransaction transaction,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] createProjectedTransaction entered. transactionId={}, payload={}", transactionId, transaction);
        try {
            ProjectedTransaction created = projectedTransactionService.createTransaction(transaction, transactionId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("X-Transaction-ID", transactionId)
                    .body(created);
        } catch (ProjectedTransactionService.DuplicateProjectedTransactionException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "DUPLICATE_PROJECTED_TRANSACTION", e.getMessage(), transactionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error creating projected transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "CREATE_ERROR", "Unexpected error", transactionId));
        }
    }

    @GetMapping
    public ResponseEntity<?> getTransactions(
            @RequestParam(value = "statementPeriod", required = false) String statementPeriod,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] getProjectedTransactions entered. transactionId={}, filters: statementPeriod={}, account={}, category={}, criticality={}, paymentMethod={}",
                transactionId, statementPeriod, account, category, criticality, paymentMethod);

        try {
            ProjectedTransactionList result = projectedTransactionService.getTransactions(statementPeriod, account, category, criticality, paymentMethod, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("[v2] Error fetching projected transactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "LIST_ERROR", "Unexpected error", transactionId));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] getProjectedTransaction entered. transactionId={}, id={}", transactionId, id);
        try {
            ProjectedTransaction tx = projectedTransactionService.getTransaction(id, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(tx);
        } catch (ProjectedTransactionService.ProjectedTransactionNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error getting projected transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "GET_ERROR", "Unexpected error", transactionId));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody ProjectedTransaction transaction,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] updateProjectedTransaction entered. transactionId={}, id={}, payload={}", transactionId, id, transaction);
        try {
            ProjectedTransaction updated = projectedTransactionService.updateTransaction(id, transaction, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(updated);
        } catch (ProjectedTransactionService.ProjectedTransactionNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", e.getMessage(), transactionId));
        } catch (ProjectedTransactionService.DuplicateProjectedTransactionException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "DUPLICATE_PROJECTED_TRANSACTION", e.getMessage(), transactionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error updating projected transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "UPDATE_ERROR", "Unexpected error", transactionId));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] deleteProjectedTransaction entered. transactionId={}, id={}", transactionId, id);
        try {
            boolean deleted = projectedTransactionService.deleteTransaction(id, transactionId);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Transaction-ID", transactionId)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Projected transaction not found for delete", transactionId));
            }
            return ResponseEntity.noContent()
                    .header("X-Transaction-ID", transactionId)
                    .build();
        } catch (Exception e) {
            logger.error("[v2] Error deleting projected transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "DELETE_ERROR", "Unexpected error", transactionId));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAllTransactions(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] deleteAllProjectedTransactions entered. transactionId={}", transactionId);
        try {
            long deletedCount = projectedTransactionService.deleteAllTransactions(transactionId);
            Map<String, Object> body = new HashMap<>();
            body.put("deletedCount", deletedCount);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(body);
        } catch (Exception e) {
            logger.error("[v2] Error deleting all projected transactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "DELETE_ALL_ERROR", "Unexpected error", transactionId));
        }
    }

    @GetMapping("/account")
    public ResponseEntity<?> getAccountProjectedTransactionList(
            @RequestParam(value = "account") String account,
            @RequestParam(value = "statementPeriod", required = false) String statementPeriod,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] getAccountProjectedTransactionList entered. transactionId={}, account={}", transactionId, account);
        try {
            AccountProjectedTransactionList result = projectedTransactionService.getAccountProjectedTransactionList(
                    account, statementPeriod, category, criticality, paymentMethod, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("[v2] Error fetching account projected transactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ACCOUNT_LIST_ERROR", "Unexpected error", transactionId));
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

