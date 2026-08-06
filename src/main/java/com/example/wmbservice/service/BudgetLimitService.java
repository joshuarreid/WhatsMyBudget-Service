package com.example.wmbservice.service;

import com.example.wmbservice.model.BudgetLimit;
import com.example.wmbservice.repository.BudgetLimitRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class BudgetLimitService {

    private static final Logger logger = LoggerFactory.getLogger(BudgetLimitService.class);
    private static final Pattern PERIOD_PATTERN = Pattern.compile(
            "^(JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER)\\d{4}$"
    );

    private final BudgetLimitRepository budgetLimitRepository;

    public BudgetLimitService(BudgetLimitRepository budgetLimitRepository) {
        this.budgetLimitRepository = budgetLimitRepository;
    }

    /**
     * Creates or updates the budget limits for an account + statement period.
     * A null limit value means "no limit set" (unconstrained) for that category.
     */
    @Transactional
    public BudgetLimit upsert(String account,
                              String statementPeriod,
                              BigDecimal essentialLimit,
                              BigDecimal nonessentialLimit,
                              BigDecimal totalLimit,
                              String transactionId) {
        String normalizedAccount = normalizeAccount(account);

        validateLimitAmount("essentialLimit", essentialLimit);
        validateLimitAmount("nonessentialLimit", nonessentialLimit);
        validateLimitAmount("totalLimit", totalLimit);

        logger.info("[budget.limits] -> upsert txId={} account={} period=BLANK", transactionId, normalizedAccount);

        BudgetLimit entity = budgetLimitRepository
                .findByAccountAndStatementPeriod(normalizedAccount, "")
                .orElseGet(() -> {
                    BudgetLimit newLimit = new BudgetLimit();
                    newLimit.setCreatedAt(LocalDateTime.now());
                    return newLimit;
                });

        entity.setAccount(normalizedAccount);
        entity.setStatementPeriod("");
        entity.setEssentialLimit(essentialLimit);
        entity.setNonessentialLimit(nonessentialLimit);
        entity.setTotalLimit(totalLimit);
        entity.setUpdatedAt(LocalDateTime.now());

        BudgetLimit saved = budgetLimitRepository.save(entity);
        logger.info("[budget.limits] <- upsert txId={} account={} period=BLANK id={}", transactionId, normalizedAccount, saved.getId());
        return saved;
    }


    /**
     * Returns the budget limits for a specific account and statement period.
     */
    public Optional<BudgetLimit> findByAccountAndPeriod(String account, String statementPeriod) {
        String normalizedPeriod = normalizePeriod(statementPeriod);
        String normalizedAccount = normalizeAccount(account);
        logger.info("[budget.limits] findByAccountAndPeriod account={} period={}", normalizedAccount, normalizedPeriod);
        return budgetLimitRepository.findByAccountAndStatementPeriod(normalizedAccount, normalizedPeriod);
    }

    /**
     * Returns all users' budget limits for a given statement period.
     */
    public List<BudgetLimit> findByPeriod(String statementPeriod) {
        logger.info("[budget.limits] findByPeriod period=ALL");
        return budgetLimitRepository.findAll();
    }

    /**
     * Returns all budget limits across all periods for a given account.
     */
    public List<BudgetLimit> findByAccount(String account) {
        String normalizedAccount = normalizeAccount(account);
        logger.info("[budget.limits] findByAccount account={}", normalizedAccount);
        return budgetLimitRepository.findByAccount(normalizedAccount);
    }

    // --- helpers ---

    private String normalizePeriod(String statementPeriod) {
        if (statementPeriod == null || statementPeriod.isBlank()) {
            throw new IllegalArgumentException("statementPeriod must not be blank");
        }
        String normalized = statementPeriod.trim().toUpperCase(Locale.ENGLISH);
        if (!PERIOD_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("statementPeriod must match FULL_MONTHYYYY format, e.g. JUNE2026");
        }
        return normalized;
    }

    private String normalizeAccount(String account) {
        if (account == null || account.isBlank()) {
            throw new IllegalArgumentException("account must not be blank");
        }
        return account.trim();
    }

    private void validateLimitAmount(String fieldName, BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must be >= 0 when provided, got: " + value);
        }
    }
}
