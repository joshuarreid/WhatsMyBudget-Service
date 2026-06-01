package com.example.wmbservice.controller;

import com.example.wmbservice.model.ProjectedTransaction;
import com.example.wmbservice.model.ProjectedTransactionList;
import com.example.wmbservice.service.ProjectedTransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for CRUD operations on ProjectedTransaction.
 * Propagates and echoes X-Transaction-ID and provides structured error responses.
 */
@CrossOrigin(origins = "http://localhost:3000", exposedHeaders = "X-Transaction-ID")
@RestController
@RequestMapping("/api/projections")
public class ProjectedTransactionController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectedTransactionController.class);
    private final ProjectedTransactionService projectedTransactionService;

    public ProjectedTransactionController(ProjectedTransactionService projectedTransactionService) {
        this.projectedTransactionService = projectedTransactionService;
    }

    /**
     * Create a projected transaction.
     *
     * @param transaction   ProjectedTransaction payload.
     * @param transactionId X-Transaction-ID header for traceability.
     * @return ResponseEntity with created transaction or error.
     */
    @PostMapping
    public ResponseEntity<?> createTransaction(
            @Valid @RequestBody ProjectedTransaction transaction,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("createProjectedTransaction entered. transactionId={}, payload={}", transactionId, transaction);
        try {
            ProjectedTransaction created = projectedTransactionService.createTransaction(transaction, transactionId);
            logger.info("createProjectedTransaction successful. transactionId={}, id={}", transactionId, created.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("X-Transaction-ID", transactionId)
                    .body(created);
        } catch (ProjectedTransactionService.DuplicateProjectedTransactionException e) {
            logger.warn("Duplicate projected transaction detected. transactionId={}, error={}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "DUPLICATE_PROJECTED_TRANSACTION", e.getMessage(), transactionId));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for createProjectedTransaction. transactionId={}, error={}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("Error creating projected transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "CREATE_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Get projected transactions with optional filters.
     *
     * @param statementPeriod Optional statement period.
     * @param account         Optional account.
     * @param category        Optional category.
     * @param criticality     Optional criticality.
     * @param paymentMethod   Optional payment method.
     * @param transactionId   X-Transaction-ID header.
     * @return ProjectedTransactionList or error.
     */
    @GetMapping
    public ResponseEntity<?> getTransactions(
            @RequestParam(value = "statementPeriod", required = false) String statementPeriod,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "criticality_id", required = false) Long criticalityId,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("getProjectedTransactions entered. transactionId={}, filters: statementPeriod={}, account={}, category={}, criticality={}, paymentMethod={}",
                transactionId, statementPeriod, account, category, criticality, paymentMethod);

        try {
            ProjectedTransactionList result = projectedTransactionService.getTransactions(statementPeriod, account, category, criticality, criticalityId, paymentMethod, transactionId);
            logger.info("getProjectedTransactions successful. transactionId={}, resultCount={}", transactionId, result.getCount());
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("Error fetching projected transactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "LIST_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Overload: get projected transactions by inclusive transactionDate range.
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

        logger.info("getProjectedTransactionsByDateRange entered. transactionId={}, startDate={}, endDate={}, account={}, category={}, criticality={}, paymentMethod={}",
                transactionId, startDate, endDate, account, category, criticality, paymentMethod);

        try {
            LocalDate s = LocalDate.parse(startDate.trim());
            LocalDate e = LocalDate.parse(endDate.trim());
            ProjectedTransactionList result = projectedTransactionService.getTransactionsByDateRange(
                    s, e, account, category, criticality, criticalityId, paymentMethod, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            logger.warn("Invalid date range in getProjectedTransactionsByDateRange. transactionId={}, startDate={}, endDate={}, error={}",
                    transactionId, startDate, endDate, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("Error fetching projected transactions by date range. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "LIST_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Get projected transaction by ID.
     *
     * @param id            Transaction ID.
     * @param transactionId X-Transaction-ID header.
     * @return ProjectedTransaction or error.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("getProjectedTransaction entered. transactionId={}, id={}", transactionId, id);
        try {
            ProjectedTransaction tx = projectedTransactionService.getTransaction(id, transactionId);
            logger.info("getProjectedTransaction successful. transactionId={}, id={}", transactionId, id);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(tx);
        } catch (ProjectedTransactionService.ProjectedTransactionNotFoundException e) {
            logger.warn("Projected transaction not found. transactionId={}, id={}", transactionId, id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("Error getting projected transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "GET_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Update a projected transaction.
     *
     * @param id            Transaction ID.
     * @param transaction   Updated fields.
     * @param transactionId X-Transaction-ID header.
     * @return Updated transaction or error.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody ProjectedTransaction transaction,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("updateProjectedTransaction entered. transactionId={}, id={}, payload={}", transactionId, id, transaction);
        try {
            ProjectedTransaction updated = projectedTransactionService.updateTransaction(id, transaction, transactionId);
            logger.info("updateProjectedTransaction successful. transactionId={}, id={}", transactionId, id);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(updated);
        } catch (ProjectedTransactionService.ProjectedTransactionNotFoundException e) {
            logger.warn("Projected transaction not found for update. transactionId={}, id={}", transactionId, id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", e.getMessage(), transactionId));
        } catch (ProjectedTransactionService.DuplicateProjectedTransactionException e) {
            logger.warn("Duplicate projected transaction on update. transactionId={}, error={}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "DUPLICATE_PROJECTED_TRANSACTION", e.getMessage(), transactionId));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid updateProjectedTransaction request. transactionId={}, error={}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("Error updating projected transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "UPDATE_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Delete a projected transaction.
     *
     * @param id            Transaction ID.
     * @param transactionId X-Transaction-ID header.
     * @return Success or error response.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("deleteProjectedTransaction entered. transactionId={}, id={}", transactionId, id);
        try {
            boolean deleted = projectedTransactionService.deleteTransaction(id, transactionId);
            if (!deleted) {
                logger.warn("Projected transaction not found for delete. transactionId={}, id={}", transactionId, id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Transaction-ID", transactionId)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Projected transaction not found for delete", transactionId));
            }
            logger.info("deleteProjectedTransaction successful. transactionId={}, id={}", transactionId, id);
            return ResponseEntity.noContent()
                    .header("X-Transaction-ID", transactionId)
                    .build();
        } catch (Exception e) {
            logger.error("Error deleting projected transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "DELETE_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Deletes all projected transactions.
     *
     * @param transactionId X-Transaction-ID header.
     * @return Count of deleted transactions.
     */
    @DeleteMapping
    public ResponseEntity<?> deleteAllTransactions(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("deleteAllProjectedTransactions entered. transactionId={}", transactionId);
        try {
            long deletedCount = projectedTransactionService.deleteAllTransactions(transactionId);
            logger.info("deleteAllProjectedTransactions successful. transactionId={}, deletedCount={}", transactionId, deletedCount);
            Map<String, Object> body = new HashMap<>();
            body.put("deletedCount", deletedCount);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(body);
        } catch (Exception e) {
            logger.error("Error deleting all projected transactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "DELETE_ALL_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Get personal and joint projected transactions for an account, matching budget transaction flow.
     */
    @GetMapping("/account")
    public ResponseEntity<?> getAccountProjectedTransactionList(
            @RequestParam(value = "account") String account,
            @RequestParam(value = "statementPeriod", required = false) String statementPeriod,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "criticality_id", required = false) Long criticalityId,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("getAccountProjectedTransactionList entered. transactionId={}, account={}", transactionId, account);
        try {
            com.example.wmbservice.model.AccountProjectedTransactionList result = projectedTransactionService.getAccountProjectedTransactionList(
                    account, statementPeriod, category, criticality, criticalityId, paymentMethod, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("Error fetching account projected transactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ACCOUNT_LIST_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Overload: account projected transactions by inclusive date range.
     */
    @GetMapping(value = "/account", params = {"account", "startDate", "endDate"})
    public ResponseEntity<?> getAccountProjectedTransactionListByDateRange(
            @RequestParam(value = "account") String account,
            @RequestParam(value = "startDate") String startDate,
            @RequestParam(value = "endDate") String endDate,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "criticality_id", required = false) Long criticalityId,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("getAccountProjectedTransactionListByDateRange entered. transactionId={}, account={}, startDate={}, endDate={}",
                transactionId, account, startDate, endDate);
        try {
            LocalDate s = LocalDate.parse(startDate.trim());
            LocalDate e = LocalDate.parse(endDate.trim());
            com.example.wmbservice.model.AccountProjectedTransactionList result = projectedTransactionService.getAccountProjectedTransactionListByDateRange(
                    account, s, e, category, criticality, criticalityId, paymentMethod, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            logger.warn("Invalid date range in getAccountProjectedTransactionListByDateRange. transactionId={}, account={}, startDate={}, endDate={}, error={}",
                    transactionId, account, startDate, endDate, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("Error fetching account projected transactions by date range. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ACCOUNT_LIST_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Standardized error response body.
     */
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
