package com.example.wmbservice.service;

import com.example.wmbservice.model.Bank;
import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.model.BulkImportResult;
import com.example.wmbservice.repository.BudgetTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BankStatementService {
    private static final Logger logger = LoggerFactory.getLogger(BankStatementService.class);

    private final BudgetTransactionRepository repository;

    /**
     * Constructs a BankStatementService with the required repository dependency.
     *
     * @param repository BudgetTransactionRepository used for persistence.
     */
    public BankStatementService(BudgetTransactionRepository repository) {
        this.repository = repository;
    }

    /**
     * Import a credit card statement file for a given bank, statement period, account,
     * and payment method. Enforces deduplication and robust logging.
     *
     * @param file            the uploaded statement CSV file
     * @param bank            the originating bank (enum)
     * @param statementPeriod required statement period for all transactions
     * @param account         account to assign to all imported transactions
     * @param paymentMethod   payment method to assign to all imported transactions
     * @param transactionId   X-Transaction-ID for logging/traceability
     * @return BulkImportResult with inserted, duplicate, and error counts/details
     */
    public BulkImportResult importCreditCardStatement(
            MultipartFile file,
            Bank bank,
            String statementPeriod,
            String account,
            String paymentMethod,
            String transactionId
    ) {
        logger.info("BankStatementService.importCreditCardStatement entered. transactionId={}, bank={}, account={}, paymentMethod={}, statementPeriod={}",
                transactionId, bank, account, paymentMethod, statementPeriod);

        if (account == null || account.isBlank()) {
            logger.warn("account is required and was not provided. transactionId={}, bank={}, statementPeriod={}, paymentMethod={}", transactionId, bank, statementPeriod, paymentMethod);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "account is required for statement import");
            return new BulkImportResult(0, 0, List.of(error));
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            logger.warn("paymentMethod is required and was not provided. transactionId={}, bank={}, account={}, statementPeriod={}", transactionId, bank, account, statementPeriod);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "paymentMethod is required for statement import");
            return new BulkImportResult(0, 0, List.of(error));
        }

        switch (bank) {
            case CHASE:
                return importChaseCreditCardStatement(file, statementPeriod, account, paymentMethod, transactionId);
            // Future: Add more banks here as needed, e.g. case AMEX: ...
            default:
                logger.warn("Unsupported bank for statement import. transactionId={}, bank={}, account={}, paymentMethod={}", transactionId, bank, account, paymentMethod);
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Bank '" + bank + "' is not supported for statement upload.");
                return new BulkImportResult(0, 0, List.of(error));
        }
    }

    /**
     * Implements import logic for Chase credit card CSV files.
     * Parses the CSV, assigns the explicit statement period, account, and paymentMethod.
     * Swaps amount signs according to Chase conventions.
     * Deduplicates using an import-time hash (name, amount, transactionDate, paymentMethod, statementPeriod).
     * Ignores user-editable fields for dedupe.
     *
     * @param file            the uploaded Chase statement
     * @param statementPeriod required statement period for transactions
     * @param account         account to assign to all transactions
     * @param paymentMethod   payment method to assign to all transactions
     * @param transactionId   for centralized logging
     * @return BulkImportResult containing counts and errors
     */
    private BulkImportResult importChaseCreditCardStatement(
            MultipartFile file,
            String statementPeriod,
            String account,
            String paymentMethod,
            String transactionId
    ) {
        logger.info("importChaseCreditCardStatement started. transactionId={}, statementPeriod={}, account={}, paymentMethod={}",
                transactionId, statementPeriod, account, paymentMethod);

        int insertedCount = 0;
        int duplicateCount = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            logger.warn("File is null or empty. transactionId={}, account={}, paymentMethod={}", transactionId, account, paymentMethod);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "CSV file is required");
            errors.add(error);
            return new BulkImportResult(0, 0, errors);
        }
        if (statementPeriod == null || statementPeriod.isBlank()) {
            logger.warn("statementPeriod is required and was not provided. transactionId={}, account={}, paymentMethod={}", transactionId, account, paymentMethod);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "statementPeriod is required for statement import");
            errors.add(error);
            return new BulkImportResult(0, 0, errors);
        }
        if (account == null || account.isBlank()) {
            logger.warn("account is required and was not provided. transactionId={}, statementPeriod={}, paymentMethod={}", transactionId, statementPeriod, paymentMethod);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "account is required for statement import");
            errors.add(error);
            return new BulkImportResult(0, 0, errors);
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            logger.warn("paymentMethod is required and was not provided. transactionId={}, statementPeriod={}, account={}", transactionId, statementPeriod, account);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "paymentMethod is required for statement import");
            errors.add(error);
            return new BulkImportResult(0, 0, errors);
        }

        DateTimeFormatter csvDateFmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateTimeFormatter csvAltDateFmt = DateTimeFormatter.ofPattern("M/d/yyyy");

        int rowNum = 1;
        String normalizedStatementPeriod = statementPeriod.trim().toUpperCase(Locale.ENGLISH);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            rowNum++;
            if (headerLine == null) {
                logger.warn("CSV file is empty. transactionId={}, account={}, paymentMethod={}", transactionId, account, paymentMethod);
                Map<String, Object> error = new HashMap<>();
                error.put("message", "CSV file is empty");
                errors.add(error);
                return new BulkImportResult(0, 0, errors);
            }

            Map<String, Integer> headerMap = new HashMap<>();
            String[] headers = headerLine.split(",", -1);
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i].trim().replace("﻿", ""), i); // Remove BOM if present
            }
            String[] requiredHeaders = {
                    "Transaction Date", "Post Date", "Description", "Category", "Type", "Amount"
            };
            for (String h : requiredHeaders) {
                if (!headerMap.containsKey(h)) {
                    logger.warn("Missing required CSV column '{}'. transactionId={}, account={}, paymentMethod={}", h, transactionId, account, paymentMethod);
                    Map<String, Object> error = new HashMap<>();
                    error.put("message", "Missing required CSV column: " + h);
                    errors.add(error);
                    return new BulkImportResult(0, 0, errors);
                }
            }

            Integer statusColIdx = headerMap.get("Status");
            boolean statusColPresent = statusColIdx != null;

            int typeColIdx = headerMap.get("Type"); // Required by contract

            int csvRowNum = 2;
            String row;
            while ((row = reader.readLine()) != null) {
                Map<String, Object> errorDetail = new HashMap<>();
                try {
                    String[] cols = row.split(",", -1);
                    if (cols.length < headers.length) {
                        errorDetail.put("row", csvRowNum);
                        errorDetail.put("message", "Skipping row: not enough columns");
                        errors.add(errorDetail);
                        logger.warn("Skipping row {}: not enough columns. transactionId={}, account={}, paymentMethod={}", csvRowNum, transactionId, account, paymentMethod);
                        csvRowNum++;
                        continue;
                    }

                    BudgetTransaction tx = new BudgetTransaction();
                    String transactionDateStr = cols[headerMap.get("Transaction Date")].trim().replace("\"", "");
                    LocalDate transactionDate;
                    try {
                        transactionDate = LocalDate.parse(transactionDateStr, csvDateFmt);
                    } catch (Exception exAlt) {
                        transactionDate = LocalDate.parse(transactionDateStr, csvAltDateFmt);
                    }
                    tx.setTransactionDate(transactionDate);
                    tx.setName(cols[headerMap.get("Description")].trim().replace("\"", ""));
                    tx.setCategory("Uncategorized");
                    tx.setCriticality("");
                    tx.setAccount(account);
                    tx.setPaymentMethod(paymentMethod);

                    if (statusColPresent && statusColIdx >= 0 && statusColIdx < cols.length) {
                        tx.setStatus(cols[statusColIdx]);
                    } else {
                        tx.setStatus(null);
                        logger.debug("Status column missing or out of bounds, row={}, account={}, paymentMethod={}", csvRowNum, account, paymentMethod);
                    }

                    tx.setCreatedTime(LocalDateTime.now());
                    tx.setStatementPeriod(normalizedStatementPeriod);

                    // Amount and TYPE SIGN LOGIC
                    String amountRawStr = cols[headerMap.get("Amount")].replace("\"", "").replaceAll("[^0-9\\.-]", "");
                    String typeStr = cols[typeColIdx].trim().toUpperCase(Locale.ENGLISH);
                    BigDecimal amount = new BigDecimal(amountRawStr);

                    // By Chase convention: SALE, FEE, INTEREST -> positive; PAYMENT, RETURN, CREDIT -> negative.
                    boolean isCredit = typeStr.contains("PAYMENT") || typeStr.contains("RETURN") || typeStr.contains("CREDIT");
                    BigDecimal original = amount;
                    if (isCredit) {
                        if (amount.compareTo(BigDecimal.ZERO) > 0) {
                            amount = amount.negate();
                            logger.debug("Amount sign swapped to negative for credit. Row={}, origType={}, origAmt={}, newAmt={}", csvRowNum, typeStr, original, amount);
                        }
                    } else {
                        if (amount.compareTo(BigDecimal.ZERO) < 0) {
                            amount = amount.abs();
                            logger.debug("Amount sign swapped to positive for debit/expense. Row={}, origType={}, origAmt={}, newAmt={}", csvRowNum, typeStr, original, amount);
                        }
                    }
                    tx.setAmount(amount);

                    // --- DEDUPE LOGIC ONLY ON IMMUTABLE CSV FIELDS ---
                    String importHash = generateImportRowHash(
                            tx.getName(),
                            amount,
                            transactionDate,
                            paymentMethod,
                            tx.getStatementPeriod()
                    );
                    tx.setRowHash(importHash);
                    logger.debug("Generated dedupe hash for row {}: {}", csvRowNum, importHash);

                    Optional<BudgetTransaction> existing = repository.findByRowHashAndStatementPeriod(
                            importHash, normalizedStatementPeriod);
                    if (existing.isPresent()) {
                        duplicateCount++;
                        logger.info("Duplicate transaction detected in import. transactionId={}, rowHash={}, row={}, account={}, paymentMethod={}", transactionId, importHash, csvRowNum, account, paymentMethod);
                        csvRowNum++;
                        continue;
                    }

                    repository.save(tx);
                    insertedCount++;
                    logger.debug("Inserted Chase transaction. transactionId={}, rowHash={}, row={}, account={}, paymentMethod={}", transactionId, importHash, csvRowNum, account, paymentMethod);
                } catch (Exception e) {
                    logger.error("Error parsing/adding CSV row {}. transactionId={}, account={}, paymentMethod={}, error={}", csvRowNum, transactionId, account, paymentMethod, e.getMessage(), e);
                    errorDetail.put("row", csvRowNum);
                    errorDetail.put("message", e.getMessage());
                    errors.add(errorDetail);
                }
                csvRowNum++;
            }

        } catch (Exception e) {
            logger.error("Unexpected error processing Chase statement CSV. transactionId={}, account={}, paymentMethod={}, error={}", transactionId, account, paymentMethod, e.getMessage(), e);
            Map<String, Object> rootErr = new HashMap<>();
            rootErr.put("message", "Unexpected error: " + e.getMessage());
            errors.add(rootErr);
            return new BulkImportResult(insertedCount, duplicateCount, errors);
        }

        logger.info("importChaseCreditCardStatement complete. transactionId={}, insertedCount={}, duplicateCount={}, errorCount={}, account={}, paymentMethod={}",
                transactionId, insertedCount, duplicateCount, errors.size(), account, paymentMethod);
        return new BulkImportResult(insertedCount, duplicateCount, errors);
    }

    /**
     * Generate a SHA-256 deduplication hash using only immutable import fields.
     * This ensures that user edits to category/criticality/account will not affect future dedupe.
     *
     * @param name Name/description of the transaction
     * @param amount Transaction amount (already sign-adjusted)
     * @param transactionDate LocalDate of transaction
     * @param paymentMethod Payment method string (from request parameter)
     * @param statementPeriod Uppercased statement period string
     * @return SHA-256 hex string
     */
    private String generateImportRowHash(
            String name,
            BigDecimal amount,
            LocalDate transactionDate,
            String paymentMethod,
            String statementPeriod
    ) {
        logger.debug("Generating dedupe hash for import: name={}, amount={}, transactionDate={}, paymentMethod={}, statementPeriod={}",
                name, amount, transactionDate, paymentMethod, statementPeriod);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String raw = String.join("|",
                    safe(name),
                    safeAmount(amount),
                    safeDate(transactionDate),
                    safe(paymentMethod),
                    safe(statementPeriod)
            );
            byte[] hash = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            logger.debug("Import row hash input: '{}', hash: '{}'", raw, sb);
            return sb.toString();
        } catch (Exception e) {
            logger.error("Error generating import row hash. error={}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate import row hash", e);
        }
    }

    private String safe(String val) { return val == null ? "" : val.trim().toLowerCase(); }
    private String safeAmount(BigDecimal val) { return val == null ? "" : val.stripTrailingZeros().toPlainString(); }
    private String safeDate(LocalDate val) { return val == null ? "" : val.toString(); }
}