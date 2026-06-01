package com.example.wmbservice.service;

import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.model.PaymentSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentSummaryService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentSummaryService.class);
    private final BudgetTransactionService budgetTransactionService;

    public PaymentSummaryService(BudgetTransactionService budgetTransactionService) {
        this.budgetTransactionService = budgetTransactionService;
    }

    /**
     * Returns a summary for each account, showing how much is owed on each credit card.
     * Accepts a list of accounts.
     */
    public List<PaymentSummaryResponse> getPaymentSummary(List<String> accountList, String statementPeriod, String transactionId) {
        logger.info("getPaymentSummary (service) entered. transactionId={}, statementPeriod={}, accounts={}", transactionId, statementPeriod, accountList);
        if (statementPeriod == null || statementPeriod.isBlank()) {
            throw new IllegalArgumentException("statementPeriod is required and must be in the form MONTHYYYY (e.g. OCTOBER2025)");
        }
        String normalizedPeriod = statementPeriod.trim().toUpperCase(Locale.ENGLISH);
        // Fetch all transactions for the relevant period
        List<BudgetTransaction> allTx = budgetTransactionService.getTransactions(normalizedPeriod, null, null, null, null, null, transactionId).getTransactions();
        List<PaymentSummaryResponse> summaries = new ArrayList<>();
        Map<String, String> normalizedAccountMap = accountList.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                acc -> acc.trim().toLowerCase(),
                acc -> acc,
                (a, b) -> a
            ));
        // Only split joint transactions between Anna and Josh
        Set<String> splitAccounts = new HashSet<>(Arrays.asList("anna", "josh"));
        for (String account : accountList) {
            if (account == null) continue;
            String normalizedAccount = account.trim().toLowerCase();
            Set<String> paymentMethods = allTx.stream()
                .map(tx -> tx.getPaymentMethod() == null ? "" : tx.getPaymentMethod().trim().toLowerCase())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
            Map<String, BigDecimal> creditCardTotals = new HashMap<>();
            Map<String, Map<String, BigDecimal>> creditCardCategoryBreakdowns = new HashMap<>();
            for (String card : paymentMethods) {
                String normalizedCard = card.trim().toLowerCase();
                BigDecimal direct = allTx.stream()
                    .filter(tx -> {
                        String txAccount = tx.getAccount();
                        String txCard = tx.getPaymentMethod();
                        // Robust normalization for both account and card
                        txAccount = txAccount == null ? "" : txAccount.trim().toLowerCase();
                        txCard = txCard == null ? "" : txCard.trim().toLowerCase();
                        return txAccount.equals(normalizedAccount) && txCard.equals(normalizedCard);
                    })
                    .map(tx -> tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal joint = allTx.stream()
                    .filter(tx -> {
                        String txAccount = tx.getAccount();
                        String txCard = tx.getPaymentMethod();
                        // Robust normalization for both account and card
                        txAccount = txAccount == null ? "" : txAccount.trim().toLowerCase();
                        txCard = txCard == null ? "" : txCard.trim().toLowerCase();
                        return txAccount.equals("joint") && txCard.equals(normalizedCard);
                    })
                    .map(tx -> tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal total = direct;
                if (splitAccounts.contains(normalizedAccount)) {
                    total = total.add(joint.divide(new BigDecimal("2.0"), 2, RoundingMode.HALF_UP));
                }
                creditCardTotals.put(normalizedCard, total);

                // Category breakdown for this card
                Map<String, BigDecimal> categoryTotals = new HashMap<>();
                // Direct transactions by category
                allTx.stream()
                    .filter(tx -> {
                        String txAccount = tx.getAccount();
                        String txCard = tx.getPaymentMethod();
                        // Robust normalization for both account and card
                        txAccount = txAccount == null ? "" : txAccount.trim().toLowerCase();
                        txCard = txCard == null ? "" : txCard.trim().toLowerCase();
                        return txAccount.equals(normalizedAccount) && txCard.equals(normalizedCard);
                    })
                    .forEach(tx -> {
                        String category = tx.getCategory() == null ? "" : tx.getCategory().trim().toLowerCase();
                        BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                        categoryTotals.put(category, categoryTotals.getOrDefault(category, BigDecimal.ZERO).add(amt));
                    });
                // Joint transactions by category (split)
                if (splitAccounts.contains(normalizedAccount)) {
                    allTx.stream()
                        .filter(tx -> {
                            String txAccount = tx.getAccount();
                            String txCard = tx.getPaymentMethod();
                            // Robust normalization for both account and card
                            txAccount = txAccount == null ? "" : txAccount.trim().toLowerCase();
                            txCard = txCard == null ? "" : txCard.trim().toLowerCase();
                            return txAccount.equals("joint") && txCard.equals(normalizedCard);
                        })
                        .forEach(tx -> {
                            String category = tx.getCategory() == null ? "" : tx.getCategory().trim().toLowerCase();
                            BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                            BigDecimal splitAmt = amt.divide(new BigDecimal("2.0"), 2, RoundingMode.HALF_UP);
                            categoryTotals.put(category, categoryTotals.getOrDefault(category, BigDecimal.ZERO).add(splitAmt));
                        });
                }
                creditCardCategoryBreakdowns.put(normalizedCard, categoryTotals);
            }
            summaries.add(new PaymentSummaryResponse(normalizedAccountMap.get(normalizedAccount), creditCardTotals, creditCardCategoryBreakdowns));
        }
        return summaries;
    }

    /**
     * Same as getPaymentSummary, but uses an inclusive transactionDate range instead of statementPeriod.
     */
    public List<PaymentSummaryResponse> getPaymentSummaryByDateRange(List<String> accountList, LocalDate startDate, LocalDate endDate, String transactionId) {
        logger.info("getPaymentSummaryByDateRange (service) entered. transactionId={}, startDate={}, endDate={}, accounts={}",
                transactionId, startDate, endDate, accountList);
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate/endDate are required and must form a valid inclusive range");
        }

        List<BudgetTransaction> allTx = budgetTransactionService.getTransactionsByDateRange(
                startDate, endDate, null, null, null, null, null, transactionId).getTransactions();

        // Reuse the existing logic by temporarily treating it as a single period's worth of data
        // (the math is based only on transaction fields).
        List<PaymentSummaryResponse> summaries = new ArrayList<>();
        Map<String, String> normalizedAccountMap = accountList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        acc -> acc.trim().toLowerCase(),
                        acc -> acc,
                        (a, b) -> a
                ));

        Set<String> splitAccounts = new HashSet<>(Arrays.asList("anna", "josh"));
        for (String account : accountList) {
            if (account == null) continue;
            String normalizedAccount = account.trim().toLowerCase();
            Set<String> paymentMethods = allTx.stream()
                    .map(tx -> tx.getPaymentMethod() == null ? "" : tx.getPaymentMethod().trim().toLowerCase())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

            Map<String, BigDecimal> creditCardTotals = new HashMap<>();
            Map<String, Map<String, BigDecimal>> creditCardCategoryBreakdowns = new HashMap<>();

            for (String card : paymentMethods) {
                String normalizedCard = card.trim().toLowerCase();
                BigDecimal direct = allTx.stream()
                        .filter(tx -> {
                            String txAccount = tx.getAccount();
                            String txCard = tx.getPaymentMethod();
                            txAccount = txAccount == null ? "" : txAccount.trim().toLowerCase();
                            txCard = txCard == null ? "" : txCard.trim().toLowerCase();
                            return txAccount.equals(normalizedAccount) && txCard.equals(normalizedCard);
                        })
                        .map(tx -> tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal joint = allTx.stream()
                        .filter(tx -> {
                            String txAccount = tx.getAccount();
                            String txCard = tx.getPaymentMethod();
                            txAccount = txAccount == null ? "" : txAccount.trim().toLowerCase();
                            txCard = txCard == null ? "" : txCard.trim().toLowerCase();
                            return txAccount.equals("joint") && txCard.equals(normalizedCard);
                        })
                        .map(tx -> tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal total = direct;
                if (splitAccounts.contains(normalizedAccount)) {
                    total = total.add(joint.divide(new BigDecimal("2.0"), 2, RoundingMode.HALF_UP));
                }
                creditCardTotals.put(normalizedCard, total);

                Map<String, BigDecimal> categoryTotals = new HashMap<>();
                allTx.stream()
                        .filter(tx -> {
                            String txAccount = tx.getAccount();
                            String txCard = tx.getPaymentMethod();
                            txAccount = txAccount == null ? "" : txAccount.trim().toLowerCase();
                            txCard = txCard == null ? "" : txCard.trim().toLowerCase();
                            return txAccount.equals(normalizedAccount) && txCard.equals(normalizedCard);
                        })
                        .forEach(tx -> {
                            String category = tx.getCategory() == null ? "" : tx.getCategory().trim().toLowerCase();
                            BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                            categoryTotals.put(category, categoryTotals.getOrDefault(category, BigDecimal.ZERO).add(amt));
                        });

                if (splitAccounts.contains(normalizedAccount)) {
                    allTx.stream()
                            .filter(tx -> {
                                String txAccount = tx.getAccount();
                                String txCard = tx.getPaymentMethod();
                                txAccount = txAccount == null ? "" : txAccount.trim().toLowerCase();
                                txCard = txCard == null ? "" : txCard.trim().toLowerCase();
                                return txAccount.equals("joint") && txCard.equals(normalizedCard);
                            })
                            .forEach(tx -> {
                                String category = tx.getCategory() == null ? "" : tx.getCategory().trim().toLowerCase();
                                BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                                BigDecimal splitAmt = amt.divide(new BigDecimal("2.0"), 2, RoundingMode.HALF_UP);
                                categoryTotals.put(category, categoryTotals.getOrDefault(category, BigDecimal.ZERO).add(splitAmt));
                            });
                }

                creditCardCategoryBreakdowns.put(normalizedCard, categoryTotals);
            }

            summaries.add(new PaymentSummaryResponse(normalizedAccountMap.get(normalizedAccount), creditCardTotals, creditCardCategoryBreakdowns));
        }
        return summaries;
    }
}
