package com.example.wmbservice.controller;

import com.example.wmbservice.model.Account;
import com.example.wmbservice.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * V2 API: Accounts endpoint.
 * Returns the list of known accounts so frontends can populate dropdowns
 * without hardcoding account names.
 */
@CrossOrigin(origins = "http://localhost:3000", exposedHeaders = "X-Transaction-ID")
@RestController
@RequestMapping("/api/v2/accounts")
public class AccountControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(AccountControllerV2.class);

    private final AccountService accountService;

    public AccountControllerV2(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Returns all known accounts (id + accountName).
     * Used by frontends to populate account dropdowns dynamically.
     */
    @GetMapping
    public ResponseEntity<List<Account>> getAccounts(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        String txId = transactionId != null && !transactionId.isBlank()
                ? transactionId
                : UUID.randomUUID().toString().replace("-", "");
        logger.info("[v2] getAccounts entered. transactionId={}", txId);
        List<Account> accounts = accountService.getAllAccounts();
        logger.info("[v2] getAccounts returning {} accounts. transactionId={}", accounts.size(), txId);
        return ResponseEntity.ok()
                .header("X-Transaction-ID", txId)
                .body(accounts);
    }
}


