package com.example.wmbservice.controller;

import com.example.wmbservice.model.LocalCache;
import com.example.wmbservice.service.LocalCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

/**
 * V2 API (JWT protected): CRUD operations on LocalCache.
 * Mirrors v1 behavior but lives under /api/v2/cache.
 */
@CrossOrigin(origins = "http://localhost:3000", exposedHeaders = "X-Transaction-ID")
@RestController
@RequestMapping("/api/v2/cache")
public class LocalCacheControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(LocalCacheControllerV2.class);
    private final LocalCacheService localCacheService;

    public LocalCacheControllerV2(LocalCacheService localCacheService) {
        this.localCacheService = localCacheService;
    }

    @GetMapping
    public ResponseEntity<List<LocalCache>> getAll(
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        logger.info("[v2] getAll entered. transactionId={}", transactionId);
        try {
            List<LocalCache> result = localCacheService.getAll();
            logger.info("[v2] getAll successful. transactionId={}, resultCount={}", transactionId, result.size());
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(result);
        } catch (Exception e) {
            logger.error("[v2] Error in getAll. transactionId={}, error={}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(null);
        }
    }

    @GetMapping("/{cacheKey}")
    public ResponseEntity<?> getByCacheKey(
            @PathVariable String cacheKey,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        logger.info("[v2] getByCacheKey entered. transactionId={}, cacheKey={}", transactionId, cacheKey);
        try {
            Optional<LocalCache> cache = localCacheService.getByCacheKey(cacheKey);
            if (cache.isPresent()) {
                logger.info("[v2] getByCacheKey successful. transactionId={}, cacheKey={}", transactionId, cacheKey);
                return ResponseEntity.ok()
                        .header("X-Transaction-ID", transactionId)
                        .body(cache.get());
            } else {
                logger.warn("[v2] Cache key not found. transactionId={}, cacheKey={}", transactionId, cacheKey);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header("X-Transaction-ID", transactionId)
                        .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", "Cache key not found", transactionId));
            }
        } catch (Exception e) {
            logger.error("[v2] Error in getByCacheKey. transactionId={}, cacheKey={}, error={}", transactionId, cacheKey, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "GET_ERROR", "Unexpected error", transactionId));
        }
    }

    @PostMapping
    public ResponseEntity<?> saveOrUpdate(
            @RequestParam String cacheKey,
            @RequestParam String cacheValue,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        logger.info("[v2] saveOrUpdate entered. transactionId={}, cacheKey={}", transactionId, cacheKey);
        try {
            LocalCache cache = localCacheService.saveOrUpdate(cacheKey, cacheValue);
            logger.info("[v2] saveOrUpdate successful. transactionId={}, cacheKey={}", transactionId, cacheKey);
            return ResponseEntity.ok()
                    .header("X-Transaction-ID", transactionId)
                    .body(cache);
        } catch (Exception e) {
            logger.error("[v2] Error in saveOrUpdate. transactionId={}, cacheKey={}, error={}", transactionId, cacheKey, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "SAVE_ERROR", "Unexpected error", transactionId));
        }
    }

    @DeleteMapping("/{cacheKey}")
    public ResponseEntity<?> deleteByCacheKey(
            @PathVariable String cacheKey,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId) {
        logger.info("[v2] deleteByCacheKey entered. transactionId={}, cacheKey={}", transactionId, cacheKey);
        try {
            localCacheService.deleteByCacheKey(cacheKey);
            logger.info("[v2] deleteByCacheKey successful. transactionId={}, cacheKey={}", transactionId, cacheKey);
            return ResponseEntity.noContent()
                    .header("X-Transaction-ID", transactionId)
                    .build();
        } catch (Exception e) {
            logger.error("[v2] Error in deleteByCacheKey. transactionId={}, cacheKey={}, error={}", transactionId, cacheKey, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("X-Transaction-ID", transactionId)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "DELETE_ERROR", "Unexpected error", transactionId));
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
