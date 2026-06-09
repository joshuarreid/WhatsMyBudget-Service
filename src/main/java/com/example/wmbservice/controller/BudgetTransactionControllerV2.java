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
 * V2 API (JWT protected): full CRUD endpoints for BudgetTransaction.
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

    @PostMapping
    public ResponseEntity<?> createTransaction(
            @Valid @RequestBody BudgetTransaction transaction,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] createTransaction entered. transactionId={}, payload={}", transactionId, transaction);

        try {
            BudgetTransaction created = budgetTransactionService.createTransaction(transaction, transactionId);
            logger.info("[v2] Transaction created successfully. transactionId={}, id={}", transactionId, created.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("X-Transaction-ID", transactionId)
                    .body(created);
        } catch (BudgetTransactionService.DuplicateTransactionException e) {
            logger.warn("[v2] Duplicate transaction detected. transactionId={}, error={}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "DUPLICATE_TRANSACTION", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error creating transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "CREATE_ERROR", "Unexpected error", transactionId));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody BudgetTransaction transaction,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] updateTransaction entered. transactionId={}, id={}, payload={}", transactionId, id, transaction);

        try {
            BudgetTransaction updated = budgetTransactionService.updateTransaction(id, transaction, transactionId);
            if (updated == null) {
                logger.warn("[v2] Transaction not found for update. transactionId={}, id={}", transactionId, id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Transaction-ID", transactionId)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Transaction not found for update", transactionId));
            }
            logger.info("[v2] updateTransaction successful. transactionId={}, id={}", transactionId, id);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(updated);
        } catch (BudgetTransactionService.DuplicateTransactionException e) {
            logger.warn("[v2] Duplicate transaction on update. transactionId={}, error={}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "DUPLICATE_TRANSACTION", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error updating transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "UPDATE_ERROR", "Unexpected error", transactionId));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] deleteTransaction entered. transactionId={}, id={}", transactionId, id);

        try {
            boolean deleted = budgetTransactionService.deleteTransaction(id, transactionId);
            if (!deleted) {
                logger.warn("[v2] Transaction not found for delete. transactionId={}, id={}", transactionId, id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Transaction-ID", transactionId)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Transaction not found for delete", transactionId));
            }
            logger.info("[v2] deleteTransaction successful. transactionId={}, id={}", transactionId, id);
            return ResponseEntity.noContent()
                    .header("X-Transaction-ID", transactionId)
                    .build();
        } catch (Exception e) {
            logger.error("[v2] Error deleting transaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "DELETE_ERROR", "Unexpected error", transactionId));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAllTransactions(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] deleteAllTransactions entered. transactionId={}", transactionId);

        try {
            long deletedCount = budgetTransactionService.deleteAllTransactions(transactionId);
            logger.info("[v2] deleteAllTransactions successful. transactionId={}, deletedCount={}", transactionId, deletedCount);
            Map<String, Object> body = Map.of("deletedCount", deletedCount);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(body);
        } catch (Exception e) {
            logger.error("[v2] Error deleting all transactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "DELETE_ALL_ERROR", "Unexpected error", transactionId));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadTransactions(
            @RequestParam("file") MultipartFile file,
            @RequestParam("statementPeriod") String statementPeriod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] uploadTransactions entered. transactionId={}, statementPeriod={}", transactionId, statementPeriod);

        try {
            BulkImportResult result = budgetTransactionService.bulkImportTransactions(
                    file, statementPeriod, transactionId);
            logger.info("[v2] uploadTransactions completed. transactionId={}, insertedCount={}, duplicateCount={}, errorCount={}",
                    transactionId, result.getInsertedCount(), result.getDuplicateCount(), result.getErrors().size());
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("[v2] Error during uploadTransactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
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

    @PostMapping("/upload-statement")
    public ResponseEntity<?> uploadCreditCardStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bank") String bank,
            @RequestParam("account") String account,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "statementPeriod") String statementPeriod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] uploadCreditCardStatement entered. transactionId={}, bank={}, account={}, paymentMethod={}, statementPeriod={}",
                transactionId, bank, account, paymentMethod, statementPeriod);

        Bank selectedBank;
        try {
            selectedBank = Bank.valueOf(bank.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            logger.warn("[v2] Invalid bank value provided. transactionId={}, providedBank={}, allowedBanks={}", transactionId, bank, Arrays.toString(Bank.values()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body("Invalid bank value. Allowed values: " + Arrays.toString(Bank.values()));
        }

        if (statementPeriod == null || statementPeriod.isBlank()) {
            logger.warn("[v2] Missing statementPeriod for credit card upload. transactionId={}, bank={}, account={}, paymentMethod={}", transactionId, bank, account, paymentMethod);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body("statementPeriod is required for credit card statement import.");
        }
        if (account == null || account.isBlank()) {
            logger.warn("[v2] Missing account for credit card upload. transactionId={}, bank={}, statementPeriod={}, paymentMethod={}", transactionId, bank, statementPeriod, paymentMethod);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body("account is required for credit card statement import.");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            logger.warn("[v2] Missing paymentMethod for credit card upload. transactionId={}, bank={}, account={}, statementPeriod={}", transactionId, bank, account, statementPeriod);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body("paymentMethod is required for credit card statement import.");
        }

        try {
            BulkImportResult result = budgetTransactionService.importCreditCardStatement(
                    file, selectedBank, statementPeriod, account, paymentMethod, transactionId);
            logger.info("[v2] uploadCreditCardStatement completed. transactionId={}, insertedCount={}, duplicateCount={}, errorCount={}",
                    transactionId, result.getInsertedCount(), result.getDuplicateCount(), result.getErrors().size());
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("[v2] Error in uploadCreditCardStatement. transactionId={}, bank={}, account={}, paymentMethod={}, error={}", transactionId, bank, account, paymentMethod, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body("Failed to upload credit card statement for bank: " + selectedBank.name());
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
