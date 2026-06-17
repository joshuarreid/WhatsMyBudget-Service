package com.example.wmbservice.service;

import com.example.wmbservice.model.ProjectedTransaction;
import com.example.wmbservice.model.ProjectedTransactionList;
import com.example.wmbservice.model.StatementPeriod;
import com.example.wmbservice.repository.ProjectedTransactionRepository;
import com.example.wmbservice.repository.StatementPeriodRepository;
import com.example.wmbservice.service.AccountService.UnknownAccountException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service layer for ProjectedTransaction CRUD with deduplication (by business-key columns),
 * validation and robust logging. Does not require a persisted row_hash column.
 *
 * Additional behavior:
 * - Validates statementPeriod format (ALLCAPSMONTHYYYY) when creating/updating projections.
 * - Ensures a corresponding StatementPeriod row exists (creates it if missing).
 */
@Service
public class ProjectedTransactionService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectedTransactionService.class);
    private final ProjectedTransactionRepository repository;
    private final StatementPeriodRepository statementPeriodRepository;
    private final CriticalityService criticalityService;
    private final AccountService accountService;

    // Regex enforces FULL MONTH NAME followed by 4-digit year, e.g. OCTOBER2025
    private static final Pattern PERIOD_NAME_PATTERN = Pattern.compile(
            "^(JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER)\\d{4}$",
            Pattern.CASE_INSENSITIVE
    );

    public ProjectedTransactionService(ProjectedTransactionRepository repository,
                                       StatementPeriodRepository statementPeriodRepository,
                                       CriticalityService criticalityService,
                                       AccountService accountService) {
        this.repository = repository;
        this.statementPeriodRepository = statementPeriodRepository;
        this.criticalityService = criticalityService;
        this.accountService = accountService;
    }

    /**
     * Exception thrown for duplicate projected transactions.
     */
    public static class DuplicateProjectedTransactionException extends RuntimeException {
        public DuplicateProjectedTransactionException(String message) { super(message); }
    }

    /**
     * Exception thrown when a projected transaction is not found.
     */
    public static class ProjectedTransactionNotFoundException extends RuntimeException {
        public ProjectedTransactionNotFoundException(String message) { super(message); }
    }

    /**
     * Create a new ProjectedTransaction, enforcing deduplication by matching business-key columns.
     * Validates and ensures the statementPeriod exists (creates it if missing).
     * If transactionDate is null (hand-add), sets it to today's date.
     *
     * @param transaction   The projected transaction to create.
     * @param transactionId X-Transaction-ID for logging/traceability.
     * @return The created ProjectedTransaction.
     */
    @Transactional
    public ProjectedTransaction createTransaction(ProjectedTransaction transaction, String transactionId) {
        logger.info("createProjectedTransaction entered. transactionId={}, payload={}", transactionId, transaction);

        if (transaction == null) {
            logger.warn("createProjectedTransaction received null payload. transactionId={}", transactionId);
            throw new IllegalArgumentException("Transaction payload must not be null");
        }

        // Ensure statementPeriod is present and valid, and ensure it exists in DB
        String rawPeriod = transaction.getStatementPeriod();
        String normalizedPeriod = normalizeAndEnsureStatementPeriod(rawPeriod, transactionId);
        transaction.setStatementPeriod(normalizedPeriod);
        criticalityService.normalize(transaction);

        // Resolve and validate account name; normalize to lowercase
        com.example.wmbservice.model.Account account = accountService.resolveByName(transaction.getAccount());
        transaction.setAccount(account.getAccountName().toLowerCase());
        transaction.setAccountId(account.getId());
        logger.debug("Resolved account '{}' -> id={}. transactionId={}", account.getAccountName(), account.getId(), transactionId);

        if (transaction.getTransactionDate() == null) {
            LocalDate now = LocalDate.now();
            transaction.setTransactionDate(now);
            logger.debug("transactionDate was null; set to current date {}. transactionId={}", now, transactionId);
        }

        transaction.setCreatedTime(LocalDateTime.now());

        // Generate transient row hash for logging and potential in-memory use
        String rowHash = generateRowHash(transaction);
        transaction.setRowHash(rowHash);
        logger.debug("Generated (transient) rowHash for projected tx. transactionId={}, rowHash={}", transactionId, rowHash);

        try {
            Optional<ProjectedTransaction> existing = repository.findByBusinessKey(
                    transaction.getName(),
                    transaction.getAccount(),
                    transaction.getAmount(),
                    transaction.getCategory(),
                    transaction.getCriticality(),
                    transaction.getTransactionDate(),
                    transaction.getPaymentMethod(),
                    transaction.getStatementPeriod()
            );

            if (existing.isPresent()) {
                logger.warn("Duplicate projected transaction detected by business-key. transactionId={}, statementPeriod={}, rowHash={}",
                        transactionId, transaction.getStatementPeriod(), rowHash);
                throw new DuplicateProjectedTransactionException("Projected transaction already exists for this statement period.");
            }

            ProjectedTransaction saved = repository.save(transaction);
            logger.info("Projected transaction created successfully. transactionId={}, id={}", transactionId, saved.getId());

            return saved;
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation on createProjectedTransaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            throw new DuplicateProjectedTransactionException("Projected transaction already exists for this statement period.");
        } catch (Exception e) {
            logger.error("Unexpected error in createProjectedTransaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get a projected transaction by ID.
     *
     * @param id            Transaction ID.
     * @param transactionId X-Transaction-ID for logging.
     * @return ProjectedTransaction if found.
     */
    @Transactional
    public ProjectedTransaction getTransaction(Long id, String transactionId) {
        logger.info("getProjectedTransaction entered. transactionId={}, id={}", transactionId, id);
        Optional<ProjectedTransaction> found = repository.findById(id);
        if (found.isPresent()) {
            logger.info("getProjectedTransaction successful. transactionId={}, id={}", transactionId, id);
            return found.get();
        } else {
            logger.warn("Projected transaction not found. transactionId={}, id={}", transactionId, id);
            throw new ProjectedTransactionNotFoundException("Projected transaction not found.");
        }
    }

    /**
     * Get projected transactions, optionally filtered.
     *
     * @param statementPeriod Optional statement period filter.
     * @param account         Optional account filter.
     * @param category        Optional category filter.
     * @param criticality     Optional criticality filter.
     * @param paymentMethod   Optional payment method filter.
     * @param transactionId   X-Transaction-ID for logging.
     * @return ProjectedTransactionList containing the results.
     */
    @Transactional
    public ProjectedTransactionList getTransactions(String statementPeriod, String account, String category, String criticality, Long criticalityId, String paymentMethod, String transactionId) {
        logger.info("getProjectedTransactions entered. transactionId={}, filters: statementPeriod={}, account={}, category={}, criticality={}, criticalityId={}, paymentMethod={}",
                transactionId, statementPeriod, account, category, criticality, criticalityId, paymentMethod);

        List<ProjectedTransaction> results;
        if (statementPeriod != null || account != null || category != null || criticality != null || paymentMethod != null) {
            logger.debug("Fetching filtered projected transactions. transactionId={}", transactionId);
            results = repository.findByFilters(statementPeriod, account, category, criticality, criticalityId, paymentMethod);
            logger.debug("Filtered projected transactions fetched. transactionId={}, count={}", transactionId, results.size());
        } else {
            logger.debug("Fetching all projected transactions. transactionId={}", transactionId);
            results = new ArrayList<>();
            repository.findAll().forEach(results::add);
            logger.debug("All projected transactions fetched. transactionId={}, count={}", transactionId, results.size());
        }

        logger.info("getProjectedTransactions successful. transactionId={}, resultCount={}", transactionId, results.size());
        return new ProjectedTransactionList(results);
    }

    /**
     * Get projected transactions by an inclusive transactionDate range, with optional filters.
     * Rows with null transactionDate are excluded.
     */
    @Transactional
    public ProjectedTransactionList getTransactionsByDateRange(LocalDate startDate,
                                                                LocalDate endDate,
                                                                String account,
                                                                String category,
                                                                String criticality,
                                                                Long criticalityId,
                                                                String paymentMethod,
                                                                String transactionId) {
        logger.info("getProjectedTransactionsByDateRange entered. transactionId={}, startDate={}, endDate={}, account={}, category={}, criticality={}, criticalityId={}, paymentMethod={}",
                transactionId, startDate, endDate, account, category, criticality, criticalityId, paymentMethod);

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate/endDate are required and must form a valid inclusive range");
        }

        List<ProjectedTransaction> results = repository.findByDateRangeFilters(startDate, endDate, account, category, criticality, criticalityId, paymentMethod);
        logger.info("getProjectedTransactionsByDateRange successful. transactionId={}, resultCount={}", transactionId, results.size());
        return new ProjectedTransactionList(results);
    }

    /**
     * Update an existing projected transaction.
     * Validates and ensures updated statementPeriod exists when provided.
     *
     * @param id            Transaction ID to update.
     * @param updated       Updated projected transaction fields.
     * @param transactionId X-Transaction-ID for logging.
     * @return Updated ProjectedTransaction.
     */
    @Transactional
    public ProjectedTransaction updateTransaction(Long id, ProjectedTransaction updated, String transactionId) {
        logger.info("updateProjectedTransaction entered. transactionId={}, id={}, payload={}", transactionId, id, updated);

        if (updated == null) {
            logger.warn("updateProjectedTransaction received null payload. transactionId={}, id={}", transactionId, id);
            throw new IllegalArgumentException("Updated transaction payload must not be null");
        }

        Optional<ProjectedTransaction> existingOpt = repository.findById(id);
        if (existingOpt.isEmpty()) {
            logger.warn("Projected transaction not found for update. transactionId={}, id={}", transactionId, id);
            throw new ProjectedTransactionNotFoundException("Projected transaction not found for update.");
        }

        ProjectedTransaction existing = existingOpt.get();

        // If a new statementPeriod is provided, validate and ensure exists
        if (updated.getStatementPeriod() != null && !updated.getStatementPeriod().isBlank()) {
            String normalized = normalizeAndEnsureStatementPeriod(updated.getStatementPeriod(), transactionId);
            existing.setStatementPeriod(normalized);
        }
        criticalityService.normalize(updated);

        // Resolve and validate account name; normalize to lowercase
        com.example.wmbservice.model.Account account = accountService.resolveByName(updated.getAccount());
        updated.setAccount(account.getAccountName().toLowerCase());
        updated.setAccountId(account.getId());
        logger.debug("Resolved account '{}' -> id={} on update. transactionId={}", account.getAccountName(), account.getId(), transactionId);

        // Copy updatable fields
        existing.setName(updated.getName());
        existing.setAmount(updated.getAmount());
        existing.setCategory(updated.getCategory());
        existing.setCriticality(updated.getCriticality());
        existing.setCriticalityId(updated.getCriticalityId());
        existing.setTransactionDate(updated.getTransactionDate());
        existing.setAccount(updated.getAccount());
        existing.setAccountId(updated.getAccountId());
        existing.setStatus(updated.getStatus());
        existing.setPaymentMethod(updated.getPaymentMethod());
        existing.setCreatedTime(updated.getCreatedTime());

        // Generate transient row hash for logging
        String newRowHash = generateRowHash(existing);
        existing.setRowHash(newRowHash);
        logger.debug("Generated (transient) new rowHash for update. transactionId={}, rowHash={}", transactionId, newRowHash);

        try {
            Optional<ProjectedTransaction> duplicate = repository.findByBusinessKey(
                    existing.getName(),
                    existing.getAccount(),
                    existing.getAmount(),
                    existing.getCategory(),
                    existing.getCriticality(),
                    existing.getTransactionDate(),
                    existing.getPaymentMethod(),
                    existing.getStatementPeriod()
            );

            if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
                logger.warn("Duplicate projected transaction detected on update by business-key. transactionId={}, rowHash={}, statementPeriod={}",
                        transactionId, newRowHash, existing.getStatementPeriod());
                throw new DuplicateProjectedTransactionException("Projected transaction already exists for this statement period.");
            }

            ProjectedTransaction saved = repository.save(existing);
            logger.info("updateProjectedTransaction successful. transactionId={}, id={}", transactionId, id);

            return saved;
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation on updateProjectedTransaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            throw new DuplicateProjectedTransactionException("Projected transaction already exists for this statement period.");
        } catch (Exception e) {
            logger.error("Unexpected error on updateProjectedTransaction. transactionId={}, error={}", transactionId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete a projected transaction by ID.
     *
     * @param id            Transaction ID to delete.
     * @param transactionId X-Transaction-ID for logging.
     * @return true if deleted, false if not found.
     */
    @Transactional
    public boolean deleteTransaction(Long id, String transactionId) {
        logger.info("deleteProjectedTransaction entered. transactionId={}, id={}", transactionId, id);
        Optional<ProjectedTransaction> existing = repository.findById(id);
        if (existing.isEmpty()) {
            logger.warn("Projected transaction not found for delete. transactionId={}, id={}", transactionId, id);
            return false;
        }
        try {
            repository.deleteById(id);
            logger.info("deleteProjectedTransaction successful. transactionId={}, id={}", transactionId, id);

            return true;
        } catch (Exception e) {
            logger.error("Error deleting projected transaction. transactionId={}, id={}, error={}", transactionId, id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete all projected transactions.
     *
     * @param transactionId X-Transaction-ID for logging.
     * @return Number of transactions deleted.
     */
    @Transactional
    public long deleteAllTransactions(String transactionId) {
        logger.info("deleteAllProjectedTransactions entered. transactionId={}", transactionId);

        // Capture ids before deletion so we can best-effort delete in RAG too.
        List<Long> idsToDelete = repository.findAll().stream()
                .map(ProjectedTransaction::getId)
                .filter(Objects::nonNull)
                .toList();

        long count = repository.count();
        if (count == 0) {
            logger.info("No projected transactions to delete. transactionId={}", transactionId);
            return 0L;
        }
        try {
            repository.deleteAll();
            logger.info("All projected transactions deleted. transactionId={}, deletedCount={}", transactionId, count);


            return count;
        } catch (Exception e) {
            logger.error("Error deleting all projected transactions. transactionId={}, error={}", transactionId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Generate SHA-256 row hash for deduplication (transient).
     * Includes only business-key fields per design: name, account, amount, category, criticality, transactionDate, paymentMethod, statementPeriod.
     *
     * @param tx ProjectedTransaction to hash.
     * @return SHA-256 hash string (lower hex).
     */
    public String generateRowHash(ProjectedTransaction tx) {
        logger.debug("generateRowHash entered for projected transaction: name={}, account={}, amount={}, category={}, criticality={}, transactionDate={}, paymentMethod={}, statementPeriod={}",
                tx.getName(), tx.getAccount(), tx.getAmount(), tx.getCategory(), tx.getCriticality(), tx.getTransactionDate(), tx.getPaymentMethod(), tx.getStatementPeriod());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String raw = String.join("|",
                    safe(tx.getName()),
                    safe(tx.getAccount()),
                    safeAmount(tx.getAmount()),
                    safe(tx.getCategory()),
                    safe(tx.getCriticality()),
                    safeDate(tx.getTransactionDate()),
                    safe(tx.getPaymentMethod()),
                    safe(tx.getStatementPeriod())
            );
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            try (Formatter formatter = new Formatter()) {
                for (byte b : hash) {
                    formatter.format("%02x", b);
                }
                String result = formatter.toString();
                logger.debug("generateRowHash successful. raw={}, hash={}", raw, result);
                return result;
            }
        } catch (Exception e) {
            logger.error("Error generating rowHash for projected transaction. error={}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate row hash", e);
        }
    }

    /**
     * Normalize the incoming periodName (trim + uppercase), validate it against the required pattern
     * and ensure a StatementPeriod row exists. If not present, creates a minimal StatementPeriod row.
     *
     * @param periodName raw incoming period name
     * @param transactionId propagated transaction id for logging
     * @return normalized period name (uppercase)
     */
    private String normalizeAndEnsureStatementPeriod(String periodName, String transactionId) {
        logger.debug("normalizeAndEnsureStatementPeriod entered. transactionId={}, rawPeriodName={}", transactionId, periodName);

        if (periodName == null) {
            logger.warn("periodName is null. transactionId={}", transactionId);
            throw new IllegalArgumentException("statementPeriod is required and must be in the form MONTHYYYY (e.g. OCTOBER2025)");
        }

        String normalized = periodName.trim().toUpperCase(Locale.ENGLISH);
        logger.debug("periodName trimmed and upper-cased. transactionId={}, normalized={}", transactionId, normalized);

        if (!PERIOD_NAME_PATTERN.matcher(normalized).matches()) {
            logger.warn("periodName failed format validation. transactionId={}, normalized={}", transactionId, normalized);
            throw new IllegalArgumentException("statementPeriod must be in the form MONTHYYYY (full month name + 4-digit year). Example: OCTOBER2025");
        }

        // Check repository for existence
        Optional<StatementPeriod> existing = statementPeriodRepository.findByPeriodName(normalized);
        if (existing.isPresent()) {
            logger.debug("StatementPeriod exists. transactionId={}, periodName={}", transactionId, normalized);
            return normalized;
        }

        // Create minimal StatementPeriod row
        try {
            StatementPeriod sp = new StatementPeriod();
            sp.setPeriodName(normalized);
            sp.setCreatedAt(LocalDateTime.now());
            // startDate/endDate left null - business rules may compute these later
            StatementPeriod saved = statementPeriodRepository.save(sp);
            logger.info("Created missing StatementPeriod. transactionId={}, id={}, periodName={}", transactionId, saved.getId(), normalized);
            return normalized;
        } catch (Exception e) {
            logger.error("Error creating StatementPeriod. transactionId={}, periodName={}, error={}", transactionId, normalized, e.getMessage(), e);
            throw new RuntimeException("Failed to create or verify statement period", e);
        }
    }

    /**
     * Returns personal and joint projected transactions for an account, splitting joint transactions for personal accounts.
     */
    public com.example.wmbservice.model.AccountProjectedTransactionList getAccountProjectedTransactionList(
            String account,
            String statementPeriod,
            String category,
            String criticality,
            Long criticalityId,
            String paymentMethod,
            String transactionId
    ) {
        logger.info("getAccountProjectedTransactionList entered. transactionId={}, account={}", transactionId, account);

        java.util.List<com.example.wmbservice.model.ProjectedTransaction> personalTxs = new java.util.ArrayList<>();
        java.util.List<com.example.wmbservice.model.ProjectedTransaction> jointTxs = new java.util.ArrayList<>();

        String normalizedAccount = account == null ? null : account.trim().toLowerCase();
        if ("joint".equalsIgnoreCase(normalizedAccount)) {
            jointTxs = repository.findByFilters(statementPeriod, "joint", category, criticality, criticalityId, paymentMethod);
        } else {
            personalTxs = repository.findByFilters(statementPeriod, normalizedAccount, category, criticality, criticalityId, paymentMethod);
            java.util.List<com.example.wmbservice.model.ProjectedTransaction> jointRaw = repository.findByFilters(statementPeriod, "joint", category, criticality, criticalityId, paymentMethod);
            for (com.example.wmbservice.model.ProjectedTransaction jt : jointRaw) {
                com.example.wmbservice.model.ProjectedTransaction split = new com.example.wmbservice.model.ProjectedTransaction();
                split.setId(jt.getId());
                split.setName("[Split] " + jt.getName());
                split.setAccount(account);
                split.setAmount(jt.getAmount() != null ? jt.getAmount().divide(new java.math.BigDecimal("2.00"), 2, java.math.RoundingMode.HALF_UP) : null);
                split.setCategory(jt.getCategory());
                split.setCriticality(jt.getCriticality());
                split.setCriticalityId(jt.getCriticalityId());
                split.setTransactionDate(jt.getTransactionDate());
                split.setStatus(jt.getStatus());
                split.setPaymentMethod(jt.getPaymentMethod());
                split.setStatementPeriod(jt.getStatementPeriod());
                split.setCreatedTime(jt.getCreatedTime());
                split.setRowHash(null);
                jointTxs.add(split);
            }
        }
        com.example.wmbservice.model.ProjectedTransactionList personalList = new com.example.wmbservice.model.ProjectedTransactionList(personalTxs);
        com.example.wmbservice.model.ProjectedTransactionList jointList = new com.example.wmbservice.model.ProjectedTransactionList(jointTxs);
        return new com.example.wmbservice.model.AccountProjectedTransactionList(personalList, jointList);
    }

    /**
     * Returns personal and joint projected transactions for an account within an inclusive date range,
     * splitting joint transactions for personal accounts.
     */
    public com.example.wmbservice.model.AccountProjectedTransactionList getAccountProjectedTransactionListByDateRange(
            String account,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            String criticality,
            Long criticalityId,
            String paymentMethod,
            String transactionId
    ) {
        logger.info("getAccountProjectedTransactionListByDateRange entered. transactionId={}, account={}, startDate={}, endDate={}",
                transactionId, account, startDate, endDate);

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate/endDate are required and must form a valid inclusive range");
        }

        java.util.List<com.example.wmbservice.model.ProjectedTransaction> personalTxs = new java.util.ArrayList<>();
        java.util.List<com.example.wmbservice.model.ProjectedTransaction> jointTxs = new java.util.ArrayList<>();

        String normalizedAccount = account == null ? null : account.trim().toLowerCase();
        if ("joint".equalsIgnoreCase(normalizedAccount)) {
            jointTxs = repository.findByDateRangeFilters(startDate, endDate, "joint", category, criticality, criticalityId, paymentMethod);
        } else {
            personalTxs = repository.findByDateRangeFilters(startDate, endDate, normalizedAccount, category, criticality, criticalityId, paymentMethod);
            java.util.List<com.example.wmbservice.model.ProjectedTransaction> jointRaw = repository.findByDateRangeFilters(startDate, endDate, "joint", category, criticality, criticalityId, paymentMethod);
            for (com.example.wmbservice.model.ProjectedTransaction jt : jointRaw) {
                com.example.wmbservice.model.ProjectedTransaction split = new com.example.wmbservice.model.ProjectedTransaction();
                split.setId(jt.getId());
                split.setName("[Split] " + jt.getName());
                split.setAccount(account);
                split.setAmount(jt.getAmount() != null ? jt.getAmount().divide(new java.math.BigDecimal("2.00"), 2, java.math.RoundingMode.HALF_UP) : null);
                split.setCategory(jt.getCategory());
                split.setCriticality(jt.getCriticality());
                split.setCriticalityId(jt.getCriticalityId());
                split.setTransactionDate(jt.getTransactionDate());
                split.setStatus(jt.getStatus());
                split.setPaymentMethod(jt.getPaymentMethod());
                split.setStatementPeriod(jt.getStatementPeriod());
                split.setCreatedTime(jt.getCreatedTime());
                split.setRowHash(null);
                jointTxs.add(split);
            }
        }
        com.example.wmbservice.model.ProjectedTransactionList personalList = new com.example.wmbservice.model.ProjectedTransactionList(personalTxs);
        com.example.wmbservice.model.ProjectedTransactionList jointList = new com.example.wmbservice.model.ProjectedTransactionList(jointTxs);
        return new com.example.wmbservice.model.AccountProjectedTransactionList(personalList, jointList);
    }

    // Helper methods for safely formatting values
    private String safe(String val) { return val == null ? "" : val.trim().toLowerCase(); }
    private String safeAmount(BigDecimal val) { return val == null ? "" : val.setScale(2, RoundingMode.HALF_UP).toString(); }
    private String safeDate(java.time.LocalDate val) { return val == null ? "" : val.toString(); }
    private String safeDateTime(LocalDateTime val) { return val == null ? "" : val.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME); }
}
