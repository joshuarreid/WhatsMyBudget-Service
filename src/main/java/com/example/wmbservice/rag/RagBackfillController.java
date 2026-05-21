package com.example.wmbservice.rag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-style endpoints to backfill the RAG store with existing DB rows.
 *
 * Endpoints:
 * - POST /api/v2/rag/backfill/projected
 * - POST /api/v2/rag/backfill/budget
 * - POST /api/v2/rag/backfill/all
 */
@CrossOrigin(origins = "http://localhost:3000", exposedHeaders = "X-Transaction-ID")
@RestController
@RequestMapping("/api/v2/rag/backfill")
public class RagBackfillController {

    private final RagBackfillService ragBackfillService;

    public RagBackfillController(RagBackfillService ragBackfillService) {
        this.ragBackfillService = ragBackfillService;
    }

    @PostMapping("/projected")
    public ResponseEntity<RagBackfillService.BackfillResult> backfillProjected(
            @RequestParam(value = "pageSize", defaultValue = "500") int pageSize,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId
    ) {
        return ResponseEntity.ok()
                .header("X-Transaction-ID", transactionId)
                .body(ragBackfillService.backfillProjected(pageSize, transactionId));
    }

    @PostMapping("/budget")
    public ResponseEntity<RagBackfillService.BackfillResult> backfillBudget(
            @RequestParam(value = "pageSize", defaultValue = "500") int pageSize,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId
    ) {
        return ResponseEntity.ok()
                .header("X-Transaction-ID", transactionId)
                .body(ragBackfillService.backfillBudget(pageSize, transactionId));
    }

    @PostMapping("/all")
    public ResponseEntity<RagBackfillService.BackfillAllResult> backfillAll(
            @RequestParam(value = "pageSize", defaultValue = "500") int pageSize,
            @RequestHeader(value = "X-Transaction-ID", required = false) String transactionId
    ) {
        return ResponseEntity.ok()
                .header("X-Transaction-ID", transactionId)
                .body(ragBackfillService.backfillAll(pageSize, transactionId));
    }
}

