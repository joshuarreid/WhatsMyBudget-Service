package com.example.wmbservice.util;

import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.service.BudgetTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;

public class RowHasher {

    private static final Logger logger = LoggerFactory.getLogger(RowHasher.class);
    // Helper methods for safely formatting values
    private static String safe(String val) { return val == null ? "" : val.trim().toLowerCase(); }
    private static String safeAmount(BigDecimal val) { return val == null ? "" : val.setScale(2, RoundingMode.HALF_UP).toString(); }
    private static String safeDate(LocalDate val) { return val == null ? "" : val.toString(); }
    private String safeDateTime(LocalDateTime val) { return val == null ? "" : val.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME); }

    /**
     * Generate SHA-256 row hash for deduplication.
     * Includes only business-key fields per design: name, account, amount, category, criticality, transactionDate, paymentMethod, statementPeriod.
     * Excludes createdTime and other non-deterministic fields to ensure deduplication works as intended.
     * @param tx BudgetTransaction to hash.
     * @return SHA-256 hash string.
     */
    public static String generateRowHash(BudgetTransaction tx) {
        logger.debug("generateRowHash entered for transaction: name={}, account={}, amount={}, category={}, criticality={}, transactionDate={}, paymentMethod={}, statementPeriod={}",
                tx.getName(), tx.getAccount(), tx.getAmount(), tx.getCategory(), tx.getCriticality(), tx.getTransactionDate(), tx.getPaymentMethod(), tx.getStatementPeriod());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String raw = String.join("|",
                    safe(tx.getName()),
                    safeAmount(tx.getAmount()),
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
            logger.error("Error generating rowHash. error={}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate row hash", e);
        }
    }
}
