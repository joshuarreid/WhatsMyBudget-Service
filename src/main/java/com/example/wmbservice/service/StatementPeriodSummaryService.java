package com.example.wmbservice.service;

import com.example.wmbservice.dto.*;
import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.model.StatementPeriod;
import com.example.wmbservice.model.StatementPeriodSummary;
import com.example.wmbservice.repository.BudgetTransactionRepository;
import com.example.wmbservice.repository.StatementPeriodRepository;
import com.example.wmbservice.repository.StatementPeriodSummaryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatementPeriodSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(StatementPeriodSummaryService.class);
    private static final int OUTLIER_LIMIT = 10;
    private static final DateTimeFormatter FULL_MONTH_PERIOD_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM")
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_MONTH_PERIOD_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM")
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final StatementPeriodSummaryRepository summaryRepository;
    private final StatementPeriodRepository statementPeriodRepository;
    private final BudgetTransactionRepository budgetTransactionRepository;
    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    public StatementPeriodSummaryService(StatementPeriodSummaryRepository summaryRepository,
                                         StatementPeriodRepository statementPeriodRepository,
                                         BudgetTransactionRepository budgetTransactionRepository,
                                         AnalyticsService analyticsService,
                                         ObjectMapper objectMapper) {
        this.summaryRepository = summaryRepository;
        this.statementPeriodRepository = statementPeriodRepository;
        this.budgetTransactionRepository = budgetTransactionRepository;
        this.analyticsService = analyticsService;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeClosedPeriodSummaries() {
        logger.info("[period.summary] -> initializeClosedPeriodSummaries");
        refreshClosedPeriodSummaries("SYSTEM");
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void scheduledRefreshClosedPeriodSummaries() {
        logger.info("[period.summary] -> scheduledRefreshClosedPeriodSummaries");
        refreshClosedPeriodSummaries("SYSTEM");
    }

    @Transactional
    public void refreshClosedPeriodSummaries(String transactionId) {
        LocalDate firstDayOfCurrentMonth = LocalDate.now().withDayOfMonth(1);
        logger.info("[period.summary] -> refreshClosedPeriodSummaries txId={} cutoff={}", transactionId, firstDayOfCurrentMonth);
        int refreshed = 0;
        for (String periodName : getKnownPeriods()) {
            StatementPeriod statementPeriod = statementPeriodRepository.findByPeriodName(periodName).orElse(null);
            if (isClosedPeriod(periodName, statementPeriod, firstDayOfCurrentMonth)) {
                upsertSummary(periodName, statementPeriod, transactionId);
                refreshed++;
            }
        }
        logger.info("[period.summary] <- refreshClosedPeriodSummaries txId={} refreshed={}", transactionId, refreshed);
    }

    @Transactional
    public AnalyticsStatementPeriodSummaryResponse getSummary(String period, String transactionId) {
        String normalizedPeriod = normalizePeriod(period);
        logger.info("[period.summary] -> getSummary txId={} period={}", transactionId, normalizedPeriod);
        StatementPeriod statementPeriod = statementPeriodRepository.findByPeriodName(normalizedPeriod).orElse(null);

        boolean knownPeriod = getKnownPeriods().contains(normalizedPeriod) || statementPeriod != null;
        if (!knownPeriod) {
            throw new IllegalArgumentException("Unknown statement period: " + normalizedPeriod);
        }

        LocalDate firstDayOfCurrentMonth = LocalDate.now().withDayOfMonth(1);
        if (isClosedPeriod(normalizedPeriod, statementPeriod, firstDayOfCurrentMonth)) {
            StatementPeriodSummary entity = summaryRepository.findByStatementPeriod(normalizedPeriod)
                    .orElseGet(() -> upsertSummary(normalizedPeriod, statementPeriod, transactionId));

            try {
                AnalyticsStatementPeriodSummaryResponse archived = toResponse(entity);
                logger.info("[period.summary] <- getSummary txId={} period={} source=archived generatedAt={}",
                        transactionId, normalizedPeriod, entity.getGeneratedAt());
                return archived;
            } catch (RuntimeException ex) {
                // Backwards-compat: older persisted summaries stored list-based JSON. Recompute and migrate.
                logger.warn("[period.summary] archived summary failed to deserialize; recomputing txId={} period={} err={}",
                        transactionId, normalizedPeriod, ex.getMessage());
                StatementPeriodSummary migrated = upsertSummary(normalizedPeriod, statementPeriod, transactionId);
                AnalyticsStatementPeriodSummaryResponse response = toResponse(migrated);
                logger.info("[period.summary] <- getSummary txId={} period={} source=archived-migrated generatedAt={}",
                        transactionId, normalizedPeriod, migrated.getGeneratedAt());
                return response;
            }
        }

        AnalyticsStatementPeriodSummaryResponse response = buildResponse(normalizedPeriod, statementPeriod, transactionId, LocalDateTime.now());
        logger.info("[period.summary] <- getSummary txId={} period={} source=live generatedAt={}",
                transactionId, normalizedPeriod, response.getGeneratedAt());
        return response;
    }

    @Transactional
    public List<AnalyticsStatementPeriodSummaryResponse> getSummariesByPeriodRange(String startPeriod,
                                                                                   String endPeriod,
                                                                                   String transactionId) {
        String normalizedStart = normalizePeriod(startPeriod);
        String normalizedEnd = normalizePeriod(endPeriod);
        logger.info("[period.summary] -> getSummariesByPeriodRange txId={} startPeriod={} endPeriod={}",
                transactionId, normalizedStart, normalizedEnd);

        PeriodBounds startBounds = resolveBounds(normalizedStart, statementPeriodRepository.findByPeriodName(normalizedStart).orElse(null));
        PeriodBounds endBounds = resolveBounds(normalizedEnd, statementPeriodRepository.findByPeriodName(normalizedEnd).orElse(null));

        if (startBounds.startDate() == null || endBounds.startDate() == null) {
            throw new IllegalArgumentException("startPeriod and endPeriod must resolve to known calendar dates");
        }
        if (startBounds.startDate().isAfter(endBounds.startDate())) {
            throw new IllegalArgumentException("startPeriod must not be after endPeriod");
        }

        List<AnalyticsStatementPeriodSummaryResponse> responses = getKnownPeriods().stream()
                .map(periodName -> new AbstractMap.SimpleEntry<>(periodName,
                        resolveBounds(periodName, statementPeriodRepository.findByPeriodName(periodName).orElse(null))))
                .filter(entry -> entry.getValue().startDate() != null)
                .filter(entry -> !entry.getValue().startDate().isBefore(startBounds.startDate())
                        && !entry.getValue().startDate().isAfter(endBounds.startDate()))
                .sorted(Comparator.comparing(entry -> entry.getValue().startDate()))
                .map(Map.Entry::getKey)
                .map(periodName -> getSummary(periodName, transactionId))
                .collect(Collectors.toList());
        logger.info("[period.summary] <- getSummariesByPeriodRange txId={} rows={}", transactionId, responses.size());
        return responses;
    }

    private StatementPeriodSummary upsertSummary(String periodName, StatementPeriod statementPeriod, String transactionId) {
        AnalyticsStatementPeriodSummaryResponse computed = buildResponse(periodName, statementPeriod, transactionId, LocalDateTime.now());
        StatementPeriodSummary entity = summaryRepository.findByStatementPeriod(periodName)
                .orElseGet(StatementPeriodSummary::new);

        entity.setStatementPeriod(periodName);
        entity.setPeriodStartDate(computed.getPeriodStartDate());
        entity.setPeriodEndDate(computed.getPeriodEndDate());
        entity.setTotalAmount(computed.getTotalAmount() != null ? computed.getTotalAmount() : BigDecimal.ZERO);
        entity.setTransactionCount(computed.getTransactionCount());
        entity.setEssentialAmount(computed.getEssentialAmount() != null ? computed.getEssentialAmount() : BigDecimal.ZERO);
        entity.setEssentialCount(computed.getEssentialCount());
        entity.setNonessentialAmount(computed.getNonessentialAmount() != null ? computed.getNonessentialAmount() : BigDecimal.ZERO);
        entity.setNonessentialCount(computed.getNonessentialCount());
        entity.setCategoryBreakdownJson(writeJson(computed.getCategoryBreakdown()));
        entity.setCriticalityBreakdownJson(writeJson(computed.getCriticalityBreakdown()));
        entity.setAccountBreakdownJson(writeJson(computed.getAccountBreakdown()));
        entity.setPaymentMethodBreakdownJson(writeJson(computed.getPaymentMethodBreakdown()));
        entity.setOutliersJson(writeJson(computed.getOutliers()));
        entity.setGeneratedAt(computed.getGeneratedAt());
        return summaryRepository.save(entity);
    }

    private AnalyticsStatementPeriodSummaryResponse buildResponse(String periodName,
                                                                 StatementPeriod statementPeriod,
                                                                 String transactionId,
                                                                 LocalDateTime generatedAt) {
        // Load all transactions for the period and group by account.
        // NOTE: We intentionally do not use resolveBounds(...) for response dates.
        List<BudgetTransaction> all = budgetTransactionRepository.findByFilters(periodName, null, null, null, null, null);
        Map<String, List<BudgetTransaction>> byAccount = all.stream()
                .collect(Collectors.groupingBy(t -> safeKey(t.getAccount()), LinkedHashMap::new, Collectors.toList()));

        LocalDate periodStartDate = all.stream()
                .map(BudgetTransaction::getTransactionDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate periodEndDate = all.stream()
                .map(BudgetTransaction::getTransactionDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        BigDecimal totalAmount = all.stream()
                .map(BudgetTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long transactionCount = all.size();

        BigDecimal essentialAmount = all.stream()
                .filter(this::isEssential)
                .map(BudgetTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long essentialCount = all.stream().filter(this::isEssential).count();

        BigDecimal nonessentialAmount = all.stream()
                .filter(this::isNonessential)
                .map(BudgetTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long nonessentialCount = all.stream().filter(this::isNonessential).count();

        Map<String, List<AnalyticsCategoryBreakdownResponse>> categoryBreakdown = new LinkedHashMap<>();
        Map<String, List<AnalyticsCriticalityBreakdownResponse>> criticalityBreakdown = new LinkedHashMap<>();
        Map<String, List<AnalyticsPaymentMethodBreakdownResponse>> paymentMethodBreakdown = new LinkedHashMap<>();
        Map<String, List<BudgetTransaction>> outliersByAccount = new LinkedHashMap<>();
        Map<String, AnalyticsAccountBreakdownResponse> accountBreakdown = new LinkedHashMap<>();

        for (Map.Entry<String, List<BudgetTransaction>> entry : byAccount.entrySet()) {
            String account = entry.getKey();
            List<BudgetTransaction> txs = entry.getValue() != null ? entry.getValue() : List.of();

            categoryBreakdown.put(account, buildCategoryBreakdown(txs));
            criticalityBreakdown.put(account, buildCriticalityBreakdown(txs));
            paymentMethodBreakdown.put(account, buildPaymentMethodBreakdown(txs));
            outliersByAccount.put(account, buildOutliers(txs, OUTLIER_LIMIT));

            BigDecimal accountTotal = txs.stream()
                    .map(BudgetTransaction::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            accountBreakdown.put(account, new AnalyticsAccountBreakdownResponse(account, accountTotal, txs.size()));
        }

        return new AnalyticsStatementPeriodSummaryResponse(
                periodName,
                periodStartDate,
                periodEndDate,
                totalAmount,
                transactionCount,
                essentialAmount,
                essentialCount,
                nonessentialAmount,
                nonessentialCount,
                categoryBreakdown,
                criticalityBreakdown,
                accountBreakdown,
                paymentMethodBreakdown,
                outliersByAccount,
                generatedAt
        );
    }

    private AnalyticsStatementPeriodSummaryResponse toResponse(StatementPeriodSummary entity) {
        return new AnalyticsStatementPeriodSummaryResponse(
                entity.getStatementPeriod(),
                entity.getPeriodStartDate(),
                entity.getPeriodEndDate(),
                defaultAmount(entity.getTotalAmount()),
                defaultCount(entity.getTransactionCount()),
                defaultAmount(entity.getEssentialAmount()),
                defaultCount(entity.getEssentialCount()),
                defaultAmount(entity.getNonessentialAmount()),
                defaultCount(entity.getNonessentialCount()),
                readJson(entity.getCategoryBreakdownJson(), new TypeReference<>() {}, Map.of()),
                readJson(entity.getCriticalityBreakdownJson(), new TypeReference<>() {}, Map.of()),
                readJson(entity.getAccountBreakdownJson(), new TypeReference<>() {}, Map.of()),
                readJson(entity.getPaymentMethodBreakdownJson(), new TypeReference<>() {}, Map.of()),
                readJson(entity.getOutliersJson(), new TypeReference<>() {}, Map.of()),
                entity.getGeneratedAt()
        );
    }

    private Set<String> getKnownPeriods() {
        Set<String> periods = new LinkedHashSet<>();
        statementPeriodRepository.findAll().stream()
                .map(StatementPeriod::getPeriodName)
                .filter(Objects::nonNull)
                .map(this::normalizePeriod)
                .forEach(periods::add);
        budgetTransactionRepository.findDistinctStatementPeriods().stream()
                .filter(Objects::nonNull)
                .map(this::normalizePeriod)
                .forEach(periods::add);
        return periods;
    }

    private boolean isClosedPeriod(String periodName, StatementPeriod statementPeriod, LocalDate firstDayOfCurrentMonth) {
        PeriodBounds bounds = resolveBounds(periodName, statementPeriod);
        if (bounds.endDate() != null) {
            return bounds.endDate().isBefore(firstDayOfCurrentMonth);
        }
        return bounds.startDate() != null && bounds.startDate().isBefore(firstDayOfCurrentMonth);
    }

    private PeriodBounds resolveBounds(String periodName, StatementPeriod statementPeriod) {
        LocalDate startDate = statementPeriod != null ? statementPeriod.getStartDate() : null;
        LocalDate endDate = statementPeriod != null ? statementPeriod.getEndDate() : null;
        if (startDate != null || endDate != null) {
            return new PeriodBounds(startDate, endDate);
        }

        YearMonth yearMonth = deriveYearMonth(periodName);
        if (yearMonth == null) {
            return new PeriodBounds(null, null);
        }
        return new PeriodBounds(yearMonth.atDay(1), yearMonth.atEndOfMonth());
    }

    private YearMonth deriveYearMonth(String periodName) {
        if (periodName == null || periodName.isBlank()) {
            return null;
        }
        String normalized = periodName.trim().toUpperCase(Locale.ENGLISH);
        try {
            return YearMonth.parse(normalized, FULL_MONTH_PERIOD_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return YearMonth.parse(normalized, SHORT_MONTH_PERIOD_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return YearMonth.parse(normalized, YEAR_MONTH_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            throw new IllegalArgumentException("period is required");
        }
        return period.trim().toUpperCase(Locale.ENGLISH);
    }

    private boolean isEssential(BudgetTransaction t) {
        if (t == null || t.getCriticality() == null) return false;
        String c = t.getCriticality().trim();
        return c.equalsIgnoreCase("ESSENTIAL") || c.equalsIgnoreCase("Essential");
    }

    private boolean isNonessential(BudgetTransaction t) {
        if (t == null || t.getCriticality() == null) return false;
        String c = t.getCriticality().trim();
        return c.equalsIgnoreCase("NONESSENTIAL") || c.equalsIgnoreCase("Nonessential");
    }

    private String safeKey(String value) {
        if (value == null) return "UNKNOWN";
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "UNKNOWN" : trimmed;
    }

    private List<AnalyticsCategoryBreakdownResponse> buildCategoryBreakdown(List<BudgetTransaction> txs) {
        Map<String, List<BudgetTransaction>> grouped = txs.stream()
                .collect(Collectors.groupingBy(t -> safeKey(t.getCategory())));
        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal total = entry.getValue().stream()
                            .map(BudgetTransaction::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new AnalyticsCategoryBreakdownResponse(entry.getKey(), total, entry.getValue().size());
                })
                .sorted(Comparator.comparing(AnalyticsCategoryBreakdownResponse::getTotalAmount, BigDecimal::compareTo).reversed())
                .toList();
    }

    private List<AnalyticsCriticalityBreakdownResponse> buildCriticalityBreakdown(List<BudgetTransaction> txs) {
        Map<String, List<BudgetTransaction>> grouped = txs.stream()
                .collect(Collectors.groupingBy(t -> safeKey(t.getCriticality())));
        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal total = entry.getValue().stream()
                            .map(BudgetTransaction::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new AnalyticsCriticalityBreakdownResponse(entry.getKey(), total, entry.getValue().size());
                })
                .sorted(Comparator.comparing(AnalyticsCriticalityBreakdownResponse::getTotalAmount, BigDecimal::compareTo).reversed())
                .toList();
    }

    private List<AnalyticsPaymentMethodBreakdownResponse> buildPaymentMethodBreakdown(List<BudgetTransaction> txs) {
        Map<String, List<BudgetTransaction>> grouped = txs.stream()
                .collect(Collectors.groupingBy(t -> safeKey(t.getPaymentMethod())));
        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal total = entry.getValue().stream()
                            .map(BudgetTransaction::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new AnalyticsPaymentMethodBreakdownResponse(entry.getKey(), total, entry.getValue().size());
                })
                .sorted(Comparator.comparing(AnalyticsPaymentMethodBreakdownResponse::getTotalAmount, BigDecimal::compareTo).reversed())
                .toList();
    }

    private List<BudgetTransaction> buildOutliers(List<BudgetTransaction> txs, int limit) {
        int safeLimit = Math.max(0, limit);
        if (safeLimit == 0 || txs == null || txs.isEmpty()) {
            return List.of();
        }
        Comparator<BudgetTransaction> comparator = Comparator
                .comparing(BudgetTransaction::getAmount, Comparator.nullsLast(BigDecimal::compareTo)).reversed()
                .thenComparing(BudgetTransaction::getTransactionDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BudgetTransaction::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        return txs.stream()
                .sorted(comparator)
                .limit(safeLimit)
                .toList();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize statement period summary", ex);
        }
    }

    private <T> T readJson(String value, TypeReference<T> typeReference, T defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to deserialize statement period summary", ex);
        }
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private long defaultCount(Long value) {
        return value != null ? value : 0L;
    }

    private record PeriodBounds(LocalDate startDate, LocalDate endDate) {
    }
}

