package com.example.wmbservice.controller;

import com.example.wmbservice.dto.BudgetLimitRequest;
import com.example.wmbservice.dto.BudgetLimitResponse;
import com.example.wmbservice.model.BudgetLimit;
import com.example.wmbservice.service.BudgetLimitService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@CrossOrigin(origins = "http://localhost:3000", exposedHeaders = "X-Transaction-ID")
@RestController
@RequestMapping("/api/v2/budget-limits")
public class BudgetLimitControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(BudgetLimitControllerV2.class);

    private final BudgetLimitService budgetLimitService;

    public BudgetLimitControllerV2(BudgetLimitService budgetLimitService) {
        this.budgetLimitService = budgetLimitService;
    }

    @PutMapping("/{account}/{statementPeriod}")
    public ResponseEntity<?> upsertBudgetLimit(
            @PathVariable String account,
            @PathVariable String statementPeriod,
            @Valid @RequestBody(required = false) BudgetLimitRequest request,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        transactionId = ensureTransactionId(transactionId);
        logger.info("[v2] upsertBudgetLimit entered. transactionId={}, account={}, statementPeriod={}", transactionId, account, statementPeriod);

        try {
            validateAccount(account);
            BudgetLimitRequest payload = request == null ? new BudgetLimitRequest() : request;
            validatePayload(payload);

            BudgetLimit saved = budgetLimitService.upsert(
                    account,
                    statementPeriod,
                    payload.getEssentialLimit(),
                    payload.getNonessentialLimit(),
                    payload.getTotalLimit(),
                    transactionId
            );

            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(toResponse(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error upserting budget limit. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "UPSERT_ERROR", "Unexpected error", transactionId));
        }
    }

    @GetMapping("/{account}/{statementPeriod}")
    public ResponseEntity<?> getBudgetLimit(
            @PathVariable String account,
            @PathVariable String statementPeriod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        transactionId = ensureTransactionId(transactionId);
        logger.info("[v2] getBudgetLimit entered. transactionId={}, account={}, statementPeriod={}", transactionId, account, statementPeriod);

        try {
            validateAccount(account);
            validateStatementPeriod(statementPeriod);

            Optional<BudgetLimit> result = budgetLimitService.findByAccountAndPeriod(account, statementPeriod);
            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Transaction-ID", transactionId)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Budget limit not found", transactionId));
            }

            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(toResponse(result.get()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error fetching budget limit. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "GET_ERROR", "Unexpected error", transactionId));
        }
    }

    @GetMapping
    public ResponseEntity<?> listBudgetLimitsByPeriod(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        transactionId = ensureTransactionId(transactionId);
        logger.info("[v2] listBudgetLimitsByPeriod entered. transactionId={}", transactionId);

        try {
            List<BudgetLimitResponse> result = budgetLimitService.findByPeriod()
                    .stream()
                    .map(BudgetLimitControllerV2::toResponse)
                    .toList();

            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error listing budget limits by period. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "LIST_ERROR", "Unexpected error", transactionId));
        }
    }

    private static String ensureTransactionId(String transactionId) {
        if (transactionId == null) {
             return UUID.randomUUID().toString().replace("-", "");
         }
        String trimmed = transactionId.trim();
        if (trimmed.isEmpty() || "N/A".equalsIgnoreCase(trimmed) || !TRANSACTION_ID_PATTERN.matcher(trimmed).matches()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return trimmed;
     }

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final Pattern PERIOD_PATTERN = Pattern.compile(
            "^(JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER)\\d{4}$"
    );
    private static final Pattern TRANSACTION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9-]{1,128}$");
    private static final int ACCOUNT_MAX_LENGTH = 64;
    private static final int PERIOD_MAX_LENGTH = 32;

    private static void validateRequiredPath(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static void validateAccount(String account) {
        validateRequiredPath(account, "account");
        String trimmed = account.trim();
        if (trimmed.length() > ACCOUNT_MAX_LENGTH) {
            throw new IllegalArgumentException("account must be <= 64 characters");
        }
        if (!ACCOUNT_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("account must contain only letters, numbers, dot, underscore, or dash");
        }
    }

    private static void validateStatementPeriod(String statementPeriod) {
        validateRequiredPath(statementPeriod, "statementPeriod");
        String normalized = statementPeriod.trim().toUpperCase(Locale.ENGLISH);
        if (normalized.length() > PERIOD_MAX_LENGTH) {
            throw new IllegalArgumentException("statementPeriod must be <= 32 characters");
        }
        if (!PERIOD_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("statementPeriod must match FULL_MONTHYYYY format, e.g. JUNE2026");
        }
    }

    private static void validatePayload(BudgetLimitRequest payload) {
        validateLimitAmount(payload.getEssentialLimit(), "essentialLimit");
        validateLimitAmount(payload.getNonessentialLimit(), "nonessentialLimit");
        validateLimitAmount(payload.getTotalLimit(), "totalLimit");
    }

    private static void validateLimitAmount(BigDecimal value, String fieldName) {
        if (value == null) {
            return;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must be >= 0 when provided");
        }
        if (value.scale() > 2) {
            throw new IllegalArgumentException(fieldName + " must have at most 2 decimal places");
        }
        if (value.precision() - value.scale() > 10) {
            throw new IllegalArgumentException(fieldName + " supports up to 10 integer digits");
        }
     }

    private static BudgetLimitResponse toResponse(BudgetLimit value) {
        return new BudgetLimitResponse(
                value.getAccount(),
                value.getStatementPeriod(),
                value.getEssentialLimit(),
                value.getNonessentialLimit(),
                value.getTotalLimit(),
                value.getCreatedAt(),
                value.getUpdatedAt()
        );
    }

    private static class ErrorResponse {
        public final int status;
        public final String code;
        public final String message;
        public final String transactionId;

        private ErrorResponse(int status, String code, String message, String transactionId) {
            this.status = status;
            this.code = code;
            this.message = message;
            this.transactionId = transactionId;
        }
    }
}
