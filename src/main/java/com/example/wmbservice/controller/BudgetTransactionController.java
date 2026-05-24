package com.example.wmbservice.controller;

import com.example.wmbservice.model.*;
import com.example.wmbservice.service.BudgetTransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for CRUD operations on BudgetTransaction.
 * Centralized logging and error handling, propagates X-Transaction-ID.
 */
@CrossOrigin(origins = "http://localhost:3000", exposedHeaders = "X-Transaction-ID")
@RestController
@RequestMapping("/api/transactions")
public class BudgetTransactionController {

    private static final Logger logger = LoggerFactory.getLogger(BudgetTransactionController.class);

    private final BudgetTransactionService budgetTransactionService;

    public BudgetTransactionController(BudgetTransactionService budgetTransactionService) {
        this.budgetTransactionService = budgetTransactionService;
    }

    /**
     * Create a budget transaction.
     * @param transaction BudgetTransaction payload.
     * @param transactionId X-Transaction-ID header for traceability.
     * @return ResponseEntity with created transaction or error.
     */
    @PostMapping
    public ResponseEntity<?> createTransaction(
            @Valid @RequestBody BudgetTransaction transaction,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("createTransaction entered. transactionId={}, payload={}", transactionId, transaction);

        try {
            BudgetTransaction created = budgetTransactionService.createTransaction(transaction, transactionId);
            logger.info("Transaction created successfully. transactionId={}, id={}", transactionId, created.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("X-Transaction-ID", transactionId)
                    .body(created);
        } catch (BudgetTransactionService.DuplicateTransactionException e) {
            logger.warn("Duplicate transaction detected. transactionId={}, error={}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "DUPLICATE_TRANSACTION", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("Error creating transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "CREATE_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Get all transactions, with optional filters.
     * @param statementPeriod Optional filter.
     * @param account Optional filter.
     * @param category Optional filter.
     * @param paymentMethod Optional filter.
     * @param transactionId X-Transaction-ID header.
     * @return BudgetTransactionList containing transactions, count and total.
     */
    @GetMapping
    public ResponseEntity<?> getTransactions(
            @RequestParam(value = "statementPeriod", required = false) String statementPeriod,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("getTransactions entered. transactionId={}, filters: statementPeriod={}, account={}, category={}, paymentMethod={}",
                transactionId, statementPeriod, account, category, paymentMethod);

        try {
            BudgetTransactionList result = budgetTransactionService.getTransactions(
                    statementPeriod, account, category, criticality, paymentMethod, transactionId);
            logger.info("getTransactions successful. transactionId={}, resultCount={}, total={}", transactionId, result.getCount(), result.getTotal());
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("Error fetching transactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "LIST_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Overload: list transactions by inclusive transactionDate range.
     * Dates must be ISO-8601 (YYYY-MM-DD).
     */
    @GetMapping(params = {"startDate", "endDate"})
    public ResponseEntity<?> getTransactionsByDateRange(
            @RequestParam(value = "startDate") String startDate,
            @RequestParam(value = "endDate") String endDate,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("getTransactionsByDateRange entered. transactionId={}, startDate={}, endDate={}, account={}, category={}, paymentMethod={}",
                transactionId, startDate, endDate, account, category, paymentMethod);

        try {
            LocalDate s = LocalDate.parse(startDate.trim());
            LocalDate e = LocalDate.parse(endDate.trim());
            BudgetTransactionList result = budgetTransactionService.getTransactionsByDateRange(
                    s, e, account, category, criticality, paymentMethod, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            logger.warn("Invalid date range in getTransactionsByDateRange. transactionId={}, startDate={}, endDate={}, error={}",
                    transactionId, startDate, endDate, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("Error fetching transactions by date range. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "LIST_ERROR", "Unexpected error", transactionId));
        }
    }


    /**
     * Get transactions for a specific account, including half of joint transactions.
     * @param account Account name (required, passed as query param).
     * @param statementPeriod Optional filter.
     * @param category Optional filter.
     * @param criticality Optional filter.
     * @param paymentMethod Optional filter.
     * @param transactionId X-Transaction-ID header.
     * @return AccountBudgetTransactionList containing personal, joint, and total transactions.
     */
    @GetMapping("/account")
    public ResponseEntity<?> getTransactionsForAccount(
            @RequestParam("account") String account,
            @RequestParam(value = "statementPeriod", required = false) String statementPeriod,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "criticality", required = false) String criticality,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId
    ) {
        logger.info("getTransactionsForAccount entered. transactionId={}, account={}", transactionId, account);

        try {
            AccountBudgetTransactionList result = budgetTransactionService.getAccountBudgetTransactionList(
                    account, statementPeriod, category, criticality, paymentMethod, transactionId);
            logger.info("getTransactionsForAccount successful. transactionId={}, account={}, personalCount={}, jointCount={}, total={}",
                    transactionId, account, result.getPersonalTransactions().getCount(), result.getJointTransactions().getCount(), result.getTotal());
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("Error in getTransactionsForAccount. transactionId={}, error={}", transactionId, e.getMessage(), e);
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
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId
    ) {
        logger.info("getTransactionsForAccountByDateRange entered. transactionId={}, account={}, startDate={}, endDate={}",
                transactionId, account, startDate, endDate);

        try {
            LocalDate s = LocalDate.parse(startDate.trim());
            LocalDate e = LocalDate.parse(endDate.trim());
            AccountBudgetTransactionList result = budgetTransactionService.getAccountBudgetTransactionListByDateRange(
                    account, s, e, category, criticality, paymentMethod, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            logger.warn("Invalid date range in getTransactionsForAccountByDateRange. transactionId={}, account={}, startDate={}, endDate={}, error={}",
                    transactionId, account, startDate, endDate, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("Error in getTransactionsForAccountByDateRange. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "ACCOUNT_TX_ERROR", "Unexpected error", transactionId));
        }
    }


    /**
     * Get transaction by ID.
     * @param id Transaction ID.
     * @param transactionId X-Transaction-ID header.
     * @return BudgetTransaction or error.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("getTransaction entered. transactionId={}, id={}", transactionId, id);

        try {
            BudgetTransaction transaction = budgetTransactionService.getTransaction(id, transactionId);
            if (transaction == null) {
                logger.warn("Transaction not found. transactionId={}, id={}", transactionId, id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Transaction-ID", transactionId)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Transaction not found", transactionId));
            }
            logger.info("getTransaction successful. transactionId={}, id={}", transactionId, id);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(transaction);
        } catch (Exception e) {
            logger.error("Error getting transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "GET_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Update a transaction.
     * @param id Transaction ID.
     * @param transaction Updated fields.
     * @param transactionId X-Transaction-ID header.
     * @return Updated transaction or error.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody BudgetTransaction transaction,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("updateTransaction entered. transactionId={}, id={}, payload={}", transactionId, id, transaction);

        try {
            BudgetTransaction updated = budgetTransactionService.updateTransaction(id, transaction, transactionId);
            if (updated == null) {
                logger.warn("Transaction not found for update. transactionId={}, id={}", transactionId, id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Transaction-ID", transactionId)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Transaction not found for update", transactionId));
            }
            logger.info("updateTransaction successful. transactionId={}, id={}", transactionId, id);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(updated);
        } catch (BudgetTransactionService.DuplicateTransactionException e) {
            logger.warn("Duplicate transaction on update. transactionId={}, error={}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "DUPLICATE_TRANSACTION", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("Error updating transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "UPDATE_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Delete a transaction.
     * @param id Transaction ID.
     * @param transactionId X-Transaction-ID header.
     * @return Success or error response.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("deleteTransaction entered. transactionId={}, id={}", transactionId, id);

        try {
            boolean deleted = budgetTransactionService.deleteTransaction(id, transactionId);
            if (!deleted) {
                logger.warn("Transaction not found for delete. transactionId={}, id={}", transactionId, id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Transaction-ID", transactionId)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Transaction not found for delete", transactionId));
            }
            logger.info("deleteTransaction successful. transactionId={}, id={}", transactionId, id);
            return ResponseEntity.noContent()
                    .header("X-Transaction-ID", transactionId)
                    .build();
        } catch (Exception e) {
            logger.error("Error deleting transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "DELETE_ERROR", "Unexpected error", transactionId));
        }
    }

    /**
     * Deletes all transactions.
     * @param transactionId X-Transaction-ID header.
     * @return Count of deleted transactions.
     */
    @DeleteMapping()
    public ResponseEntity<?> deleteAllTransactions(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("deleteAllTransactions entered. transactionId={}", transactionId);

        try {
            long deletedCount = budgetTransactionService.deleteAllTransactions(transactionId);
            logger.info("deleteAllTransactions successful. transactionId={}, deletedCount={}", transactionId, deletedCount);

            Map<String, Object> body = Map.of("deletedCount", deletedCount);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(body);
        } catch (Exception e) {
            logger.error("Error deleting all transactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "DELETE_ALL_ERROR", "Unexpected error", transactionId));
        }
    }


    /**
     * Uploads a CSV of transactions for bulk import with deduplication and validation.
     * Accepts multipart form-data: file (CSV), statementPeriod (required), and X-Transaction-ID (header).
     * Returns inserted count, duplicate count, and error details.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadTransactions(
            @RequestParam("file") MultipartFile file,
            @RequestParam("statementPeriod") String statementPeriod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("uploadTransactions entered. transactionId={}, statementPeriod={}", transactionId, statementPeriod);

        try {
            BulkImportResult result = budgetTransactionService.bulkImportTransactions(
                    file, statementPeriod, transactionId);

            logger.info("uploadTransactions completed. transactionId={}, insertedCount={}, duplicateCount={}, errorCount={}",
                    transactionId, result.getInsertedCount(), result.getDuplicateCount(), result.getErrors().size());

            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("Error during uploadTransactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "CSV_IMPORT_ERROR");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("transactionId", transactionId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(errorResponse);
        }
    }


    /**
     * Upload a credit card statement CSV file. Requires specifying originating bank, statement period, account, and payment method.
     * This endpoint supports repeated uploads for the same statement period and skips already-imported transactions.
     *
     * @param file            the CSV file being uploaded
     * @param bank            the source bank; determines parsing logic (must match a supported enum)
     * @param statementPeriod required statement period for associating transactions (no inference from file)
     * @param account         the account associated with the uploaded transactions
     * @param paymentMethod   the payment method associated with the uploaded transactions
     * @param transactionId   X-Transaction-ID for logging/traceability
     * @return summary response of inserted, duplicate, and error counts
     */
    @PostMapping("/upload-statement")
    public ResponseEntity<?> uploadCreditCardStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bank") String bank,
            @RequestParam("account") String account,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "statementPeriod", required = true) String statementPeriod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId
    ) {
        logger.info("uploadCreditCardStatement entered. transactionId={}, bank={}, account={}, paymentMethod={}, statementPeriod={}",
                transactionId, bank, account, paymentMethod, statementPeriod);

        Bank selectedBank;
        try {
            selectedBank = Bank.valueOf(bank.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            logger.warn("Invalid bank value provided. transactionId={}, providedBank={}, allowedBanks={}", transactionId, bank, Arrays.toString(Bank.values()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body("Invalid bank value. Allowed values: " + Arrays.toString(Bank.values()));
        }

        if (statementPeriod == null || statementPeriod.isBlank()) {
            logger.warn("Missing statementPeriod for credit card upload. transactionId={}, bank={}, account={}, paymentMethod={}", transactionId, bank, account, paymentMethod);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body("statementPeriod is required for credit card statement import.");
        }
        if (account == null || account.isBlank()) {
            logger.warn("Missing account for credit card upload. transactionId={}, bank={}, statementPeriod={}, paymentMethod={}", transactionId, bank, statementPeriod, paymentMethod);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body("account is required for credit card statement import.");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            logger.warn("Missing paymentMethod for credit card upload. transactionId={}, bank={}, account={}, statementPeriod={}", transactionId, bank, account, statementPeriod);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body("paymentMethod is required for credit card statement import.");
        }

        try {
            BulkImportResult result =
                    budgetTransactionService.importCreditCardStatement(file, selectedBank, statementPeriod, account, paymentMethod, transactionId);

            logger.info("uploadCreditCardStatement completed. transactionId={}, insertedCount={}, duplicateCount={}, errorCount={}",
                    transactionId, result.getInsertedCount(), result.getDuplicateCount(), result.getErrors().size());

            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("Error in uploadCreditCardStatement. transactionId={}, bank={}, account={}, paymentMethod={}, error={}", transactionId, bank, account, paymentMethod, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body("Failed to upload credit card statement for bank: " + selectedBank.name());
        }
    }

    /**
     * Error response format for all error cases.
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
