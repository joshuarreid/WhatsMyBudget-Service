package com.example.wmbservice.rag;

import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.model.ProjectedTransaction;
import com.example.wmbservice.repository.BudgetTransactionRepository;
import com.example.wmbservice.repository.ProjectedTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Backfill service to ingest all existing transactions into the FastAPI RAG store.
 *
 * Note: This is intended as an admin/one-time (or occasional) operation.
 * Ingest calls are best-effort (the RagDocumentClient swallows exceptions).
 */
@Service
public class RagBackfillService {
    private static final Logger logger = LoggerFactory.getLogger(RagBackfillService.class);

    private final ProjectedTransactionRepository projectedRepo;
    private final BudgetTransactionRepository budgetRepo;
    private final RagDocumentClient ragDocumentClient;

    public RagBackfillService(
            ProjectedTransactionRepository projectedRepo,
            BudgetTransactionRepository budgetRepo,
            RagDocumentClient ragDocumentClient
    ) {
        this.projectedRepo = projectedRepo;
        this.budgetRepo = budgetRepo;
        this.ragDocumentClient = ragDocumentClient;
    }

    public BackfillResult backfillProjected(int pageSize, String transactionId) {
        long attempted = 0;
        int page = 0;

        while (true) {
            Page<ProjectedTransaction> batch = projectedRepo.findAll(PageRequest.of(page, pageSize));
            if (batch.isEmpty()) break;

            for (ProjectedTransaction tx : batch.getContent()) {
                ragDocumentClient.ingestProjected(tx, transactionId);
                attempted++;
            }

            if (!batch.hasNext()) break;
            page++;
        }

        logger.info("RAG backfill projected complete. transactionId={}, attempted={}", transactionId, attempted);
        return new BackfillResult(attempted, pageSize);
    }

    public BackfillResult backfillBudget(int pageSize, String transactionId) {
        long attempted = 0;
        int page = 0;

        while (true) {
            Page<BudgetTransaction> batch = budgetRepo.findAll(PageRequest.of(page, pageSize));
            if (batch.isEmpty()) break;

            for (BudgetTransaction tx : batch.getContent()) {
                ragDocumentClient.ingestBudget(tx, transactionId);
                attempted++;
            }

            if (!batch.hasNext()) break;
            page++;
        }

        logger.info("RAG backfill budget complete. transactionId={}, attempted={}", transactionId, attempted);
        return new BackfillResult(attempted, pageSize);
    }

    public BackfillAllResult backfillAll(int pageSize, String transactionId) {
        BackfillResult projected = backfillProjected(pageSize, transactionId);
        BackfillResult budget = backfillBudget(pageSize, transactionId);
        return new BackfillAllResult(projected.attempted(), budget.attempted(), pageSize);
    }

    public record BackfillResult(long attempted, int pageSize) {
    }

    public record BackfillAllResult(long projectedAttempted, long budgetAttempted, int pageSize) {
    }
}

