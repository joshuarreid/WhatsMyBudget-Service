package com.example.wmbservice.rag;

import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.model.ProjectedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal client for the FastAPI RAG service.
 *
 * Contract:
 * - Create: POST /ingest with {doc_id, text, metadata}
 * - Update: PUT /documents/{doc_id} with {text, metadata}
 * - Delete: DELETE /documents/{doc_id}
 *
 * Notes:
 * - Best-effort: exceptions are caught and logged so business mutations still succeed.
 * - doc_id is deterministic from the ProjectedTransaction id.
 */
public class RagDocumentClient {
    private static final Logger logger = LoggerFactory.getLogger(RagDocumentClient.class);

    private final RestClient restClient;
    private final String ragBaseUrl;

    public RagDocumentClient(RestClient restClient, String ragBaseUrl) {
        this.restClient = restClient;
        this.ragBaseUrl = (ragBaseUrl == null || ragBaseUrl.isBlank())
                ? "http://localhost:8080"
                : ragBaseUrl.replaceAll("/+$", "");
    }

    public void ingestProjected(ProjectedTransaction tx, String transactionId) {
        if (tx == null || tx.getId() == null) return;

        String docId = projectedDocId(tx.getId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("doc_id", docId);
        payload.put("text", projectedAsText(tx));
        payload.put("metadata", projectedMetadata(tx));

        try {
            restClient.post()
                    .uri(ragBaseUrl + "/ingest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Transaction-ID", safe(transactionId))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.warn("RAG ingest failed. docId={}, transactionId={}, err={}", docId, safe(transactionId), e.getMessage());
        }
    }

    public void updateProjected(ProjectedTransaction tx, String transactionId) {
        if (tx == null || tx.getId() == null) return;

        String docId = projectedDocId(tx.getId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", projectedAsText(tx));
        payload.put("metadata", projectedMetadata(tx));

        try {
            restClient.put()
                    .uri(ragBaseUrl + "/documents/" + docId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Transaction-ID", safe(transactionId))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.warn("RAG update failed. docId={}, transactionId={}, err={}", docId, safe(transactionId), e.getMessage());
        }
    }

    public void deleteProjected(Long projectedId, String transactionId) {
        if (projectedId == null) return;

        String docId = projectedDocId(projectedId);

        try {
            restClient.delete()
                    .uri(ragBaseUrl + "/documents/" + docId)
                    .header("X-Transaction-ID", safe(transactionId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.warn("RAG delete failed. docId={}, transactionId={}, err={}", docId, safe(transactionId), e.getMessage());
        }
    }

    public void ingestBudget(BudgetTransaction tx, String transactionId) {
        if (tx == null || tx.getId() == null) return;

        String docId = budgetDocId(tx.getId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("doc_id", docId);
        payload.put("text", budgetAsText(tx));
        payload.put("metadata", budgetMetadata(tx));

        try {
            restClient.post()
                    .uri(ragBaseUrl + "/ingest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Transaction-ID", safe(transactionId))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.warn("RAG ingest failed (budget). docId={}, transactionId={}, err={}", docId, safe(transactionId), e.getMessage());
        }
    }

    public void updateBudget(BudgetTransaction tx, String transactionId) {
        if (tx == null || tx.getId() == null) return;

        String docId = budgetDocId(tx.getId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", budgetAsText(tx));
        payload.put("metadata", budgetMetadata(tx));

        try {
            restClient.put()
                    .uri(ragBaseUrl + "/documents/" + docId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Transaction-ID", safe(transactionId))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.warn("RAG update failed (budget). docId={}, transactionId={}, err={}", docId, safe(transactionId), e.getMessage());
        }
    }

    public void deleteBudget(Long budgetId, String transactionId) {
        if (budgetId == null) return;

        String docId = budgetDocId(budgetId);

        try {
            restClient.delete()
                    .uri(ragBaseUrl + "/documents/" + docId)
                    .header("X-Transaction-ID", safe(transactionId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.warn("RAG delete failed (budget). docId={}, transactionId={}, err={}", docId, safe(transactionId), e.getMessage());
        }
    }

    private static String projectedDocId(Long id) {
        return "projected-" + sha256Hex24("projected|" + id);
    }

    private static String projectedAsText(ProjectedTransaction tx) {
        // Keep this deterministic, embedding-friendly, and focused on searchable business fields.
        StringBuilder sb = new StringBuilder();
        sb.append("type: projected_transaction\n");
        sb.append("id: ").append(tx.getId()).append("\n");
        sb.append("name: ").append(safe(tx.getName())).append("\n");
        sb.append("account: ").append(safe(tx.getAccount())).append("\n");
        sb.append("statementPeriod: ").append(safe(tx.getStatementPeriod())).append("\n");
        sb.append("category: ").append(safe(tx.getCategory())).append("\n");
        sb.append("criticality: ").append(safe(tx.getCriticality())).append("\n");
        if (tx.getPaymentMethod() != null && !tx.getPaymentMethod().isBlank()) {
            sb.append("paymentMethod: ").append(safe(tx.getPaymentMethod())).append("\n");
        }
        if (tx.getTransactionDate() != null) {
            sb.append("transactionDate: ").append(tx.getTransactionDate()).append("\n");
        }
        BigDecimal amount = tx.getAmount();
        if (amount != null) {
            sb.append("amount: ").append(amount).append("\n");
        }
        if (tx.getStatus() != null && !tx.getStatus().isBlank()) {
            sb.append("status: ").append(safe(tx.getStatus())).append("\n");
        }
        return sb.toString().trim();
    }

    private static Map<String, Object> projectedMetadata(ProjectedTransaction tx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", "projected_transaction");
        m.put("source", "wmbservice");
        m.put("id", tx.getId());
        m.put("account", tx.getAccount());
        m.put("statementPeriod", tx.getStatementPeriod());
        m.put("category", tx.getCategory());
        m.put("criticality", tx.getCriticality());
        m.put("paymentMethod", tx.getPaymentMethod());
        m.put("status", tx.getStatus());
        return m;
    }

    private static String budgetDocId(Long id) {
        return "budget-" + sha256Hex24("budget|" + id);
    }

    private static String budgetAsText(BudgetTransaction tx) {
        StringBuilder sb = new StringBuilder();
        sb.append("type: budget_transaction\n");
        sb.append("id: ").append(tx.getId()).append("\n");
        sb.append("name: ").append(safe(tx.getName())).append("\n");
        sb.append("account: ").append(safe(tx.getAccount())).append("\n");
        sb.append("statementPeriod: ").append(safe(tx.getStatementPeriod())).append("\n");
        sb.append("category: ").append(safe(tx.getCategory())).append("\n");
        sb.append("criticality: ").append(safe(tx.getCriticality())).append("\n");
        if (tx.getPaymentMethod() != null && !tx.getPaymentMethod().isBlank()) {
            sb.append("paymentMethod: ").append(safe(tx.getPaymentMethod())).append("\n");
        }
        if (tx.getTransactionDate() != null) {
            sb.append("transactionDate: ").append(tx.getTransactionDate()).append("\n");
        }
        if (tx.getAmount() != null) {
            sb.append("amount: ").append(tx.getAmount()).append("\n");
        }
        if (tx.getStatus() != null && !tx.getStatus().isBlank()) {
            sb.append("status: ").append(safe(tx.getStatus())).append("\n");
        }
        return sb.toString().trim();
    }

    private static Map<String, Object> budgetMetadata(BudgetTransaction tx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", "budget_transaction");
        m.put("source", "wmbservice");
        m.put("id", tx.getId());
        m.put("account", tx.getAccount());
        m.put("statementPeriod", tx.getStatementPeriod());
        m.put("category", tx.getCategory());
        m.put("criticality", tx.getCriticality());
        m.put("paymentMethod", tx.getPaymentMethod());
        m.put("status", tx.getStatus());
        // rowHash is useful for debugging/dedupe audits
        m.put("rowHash", tx.getRowHash());
        return m;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String sha256Hex24(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 24);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
