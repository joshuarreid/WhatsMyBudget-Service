package com.example.wmbservice.controller;

import com.example.wmbservice.model.StatementPeriod;
import com.example.wmbservice.service.StatementPeriodService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V2 API (JWT protected): CRUD operations on StatementPeriod.
 * Mirrors v1 behavior but lives under /api/v2/statements.
 */
@CrossOrigin(origins = "http://localhost:3000", exposedHeaders = "X-Transaction-ID")
@RestController
@RequestMapping("/api/v2/statements")
public class StatementPeriodControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(StatementPeriodControllerV2.class);

    private final StatementPeriodService statementPeriodService;

    public StatementPeriodControllerV2(StatementPeriodService statementPeriodService) {
        this.statementPeriodService = statementPeriodService;
    }

    @PostMapping
    public ResponseEntity<?> createStatementPeriod(
            @Valid @RequestBody StatementPeriod statementPeriod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] createStatementPeriod entered. transactionId={}, payload={}", transactionId, statementPeriod);

        try {
            StatementPeriod created = statementPeriodService.createStatementPeriod(statementPeriod, transactionId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("X-Transaction-ID", transactionId)
                    .body(created);
        } catch (StatementPeriodService.DuplicateStatementPeriodException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "DUPLICATE_STATEMENT_PERIOD", e.getMessage(), transactionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error creating statement period. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "CREATE_ERROR", "Unexpected error", transactionId));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllStatementPeriods(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] getAllStatementPeriods entered. transactionId={}", transactionId);

        try {
            List<StatementPeriod> result = statementPeriodService.getAllStatementPeriods(transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("[v2] Error fetching statement periods. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "LIST_ERROR", "Unexpected error", transactionId));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStatementPeriod(
            @PathVariable Long id,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] getStatementPeriod entered. transactionId={}, id={}", transactionId, id);

        try {
            StatementPeriod sp = statementPeriodService.getStatementPeriod(id, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(sp);
        } catch (StatementPeriodService.StatementPeriodNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error getting statement period. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "GET_ERROR", "Unexpected error", transactionId));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStatementPeriod(
            @PathVariable Long id,
            @Valid @RequestBody StatementPeriod statementPeriod,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] updateStatementPeriod entered. transactionId={}, id={}, payload={}", transactionId, id, statementPeriod);

        try {
            StatementPeriod updated = statementPeriodService.updateStatementPeriod(id, statementPeriod, transactionId);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(updated);
        } catch (StatementPeriodService.StatementPeriodNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", e.getMessage(), transactionId));
        } catch (StatementPeriodService.DuplicateStatementPeriodException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "DUPLICATE_STATEMENT_PERIOD", e.getMessage(), transactionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", e.getMessage(), transactionId));
        } catch (Exception e) {
            logger.error("[v2] Error updating statement period. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "UPDATE_ERROR", "Unexpected error", transactionId));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStatementPeriod(
            @PathVariable Long id,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] deleteStatementPeriod entered. transactionId={}, id={}", transactionId, id);

        try {
            boolean deleted = statementPeriodService.deleteStatementPeriod(id, transactionId);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Transaction-ID", transactionId)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Statement period not found for delete", transactionId));
            }
            return ResponseEntity.noContent()
                    .header("X-Transaction-ID", transactionId)
                    .build();
        } catch (Exception e) {
            logger.error("[v2] Error deleting statement period. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "DELETE_ERROR", "Unexpected error", transactionId));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAllStatementPeriods(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {

        logger.info("[v2] deleteAllStatementPeriods entered. transactionId={}", transactionId);

        try {
            long deletedCount = statementPeriodService.deleteAllStatementPeriods(transactionId);
            Map<String, Object> body = new HashMap<>();
            body.put("deletedCount", deletedCount);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(body);
        } catch (Exception e) {
            logger.error("[v2] Error deleting all statement periods. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "DELETE_ALL_ERROR", "Unexpected error", transactionId));
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

