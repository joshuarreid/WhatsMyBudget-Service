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
            logger.info("[period.summary] <- getSummary txId={} period={} source=archived generatedAt={}",
                    transactionId, normalizedPeriod, entity.getGeneratedAt());
            return toResponse(entity);
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
        AnalyticsPeriodOverviewResponse overview = analyticsService.getPeriodOverview(periodName, null, null, transactionId);
        List<AnalyticsCategoryBreakdownResponse> categories = analyticsService.getCategoryBreakdown(periodName, null, null, transactionId);
        List<AnalyticsCriticalityBreakdownResponse> criticalities = analyticsService.getCriticalityBreakdown(periodName, null, null, transactionId);
        List<AnalyticsAccountBreakdownResponse> accounts = analyticsService.getAccountBreakdown(periodName, null, transactionId);
        List<AnalyticsPaymentMethodBreakdownResponse> paymentMethods = analyticsService.getPaymentMethodBreakdown(periodName, null, transactionId);
        List<BudgetTransaction> outliers = analyticsService.getOutliers(periodName, OUTLIER_LIMIT, transactionId);
        PeriodBounds bounds = resolveBounds(periodName, statementPeriod);

        return new AnalyticsStatementPeriodSummaryResponse(
                periodName,
                bounds.startDate(),
                bounds.endDate(),
                overview.getTotalAmount() != null ? overview.getTotalAmount() : BigDecimal.ZERO,
                overview.getTransactionCount(),
                amountForCriticality(criticalities, "ESSENTIAL"),
                countForCriticality(criticalities, "ESSENTIAL"),
                amountForCriticality(criticalities, "NONESSENTIAL"),
                countForCriticality(criticalities, "NONESSENTIAL"),
                categories,
                criticalities,
                accounts,
                paymentMethods,
                outliers,
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
                readJson(entity.getCategoryBreakdownJson(), new TypeReference<>() {}),
                readJson(entity.getCriticalityBreakdownJson(), new TypeReference<>() {}),
                readJson(entity.getAccountBreakdownJson(), new TypeReference<>() {}),
                readJson(entity.getPaymentMethodBreakdownJson(), new TypeReference<>() {}),
                readJson(entity.getOutliersJson(), new TypeReference<>() {}),
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

    private BigDecimal amountForCriticality(List<AnalyticsCriticalityBreakdownResponse> rows, String value) {
        return rows.stream()
                .filter(row -> row.getCriticality() != null && row.getCriticality().trim().equalsIgnoreCase(value))
                .map(AnalyticsCriticalityBreakdownResponse::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countForCriticality(List<AnalyticsCriticalityBreakdownResponse> rows, String value) {
        return rows.stream()
                .filter(row -> row.getCriticality() != null && row.getCriticality().trim().equalsIgnoreCase(value))
                .mapToLong(AnalyticsCriticalityBreakdownResponse::getTransactionCount)
                .sum();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize statement period summary", ex);
        }
    }

    private <T> List<T> readJson(String value, TypeReference<List<T>> typeReference) {
        if (value == null || value.isBlank()) {
            return List.of();
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


