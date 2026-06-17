package com.example.wmbservice.service;

import com.example.wmbservice.model.Account;
import com.example.wmbservice.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for resolving account names to Account entities.
 * All write paths must call resolveByName() to validate account names
 * and obtain the canonical account_id before persisting transactions.
 */
@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Exception thrown when an account name is not found in the accounts table.
     * Controllers should catch this and return 400 BAD_REQUEST.
     */
    public static class UnknownAccountException extends RuntimeException {
        public UnknownAccountException(String accountName) {
            super("Unknown account: '" + accountName + "'. Account must exist in the accounts table.");
        }
    }

    /**
     * Resolves an account name to its Account entity.
     * Trims and lowercases the input before lookup (case-insensitive matching).
     *
     * @param accountName the raw account name from request or CSV
     * @return the matched Account
     * @throws IllegalArgumentException  if accountName is null or blank
     * @throws UnknownAccountException   if no account with that name exists
     */
    public Account resolveByName(String accountName) {
        if (accountName == null || accountName.isBlank()) {
            throw new IllegalArgumentException("account name must not be null or blank");
        }

        String normalized = accountName.trim().toLowerCase();
        logger.debug("resolveByName: normalized='{}' (raw='{}')", normalized, accountName);

        return accountRepository.findByAccountNameIgnoreCase(normalized)
                .orElseThrow(() -> {
                    logger.warn("resolveByName: unknown account '{}'", normalized);
                    return new UnknownAccountException(normalized);
                });
    }

    /**
     * Returns all known accounts. Used by the accounts endpoint for frontend dropdowns.
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }
}

