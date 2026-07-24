package com.example.wmbservice.service;

import com.example.wmbservice.model.IncomeTransaction;
import com.example.wmbservice.model.IncomeTransactionList;
import com.example.wmbservice.repository.IncomeTransactionRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
public class IncomeTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(IncomeTransactionService.class);

    private final IncomeTransactionRepository repository;

    public IncomeTransactionService(IncomeTransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IncomeTransaction createTransaction(IncomeTransaction transaction, String transactionId) {
        logger.info("createIncomeTransaction entered. transactionId={}", transactionId);

        if (transaction == null) {
            throw new IllegalArgumentException("Transaction payload is required");
        }
        if (transaction.getTransactionDate() == null) {
            throw new IllegalArgumentException("transactionDate is required");
        }
        if (transaction.getId() != null) {
            throw new IllegalArgumentException("id must not be provided for create operations");
        }

        transaction.setStatementPeriod(normalizeStatementPeriod(transaction.getStatementPeriod(), transaction.getTransactionDate()));
        transaction.setAccount(normalize(transaction.getAccount()));
        validateRecurringFields(transaction);

        LocalDateTime now = LocalDateTime.now();
        transaction.setCreatedTime(now);
        transaction.setUpdatedTime(now);

        return repository.save(transaction);
    }

    @Transactional
    public IncomeTransactionList getTransactions(String statementPeriod,
                                                 String account,
                                                 Boolean recurringMonthly,
                                                 String transactionId) {
        logger.info("getIncomeTransactions entered. transactionId={}, statementPeriod={}, account={}, recurringMonthly={}",
                transactionId, statementPeriod, account, recurringMonthly);

        List<IncomeTransaction> transactions;
        if ((statementPeriod == null || statementPeriod.isBlank())
                && (account == null || account.isBlank())
                && recurringMonthly == null) {
            transactions = repository.findAll();
        } else {
            String normalizedPeriod = statementPeriod == null || statementPeriod.isBlank()
                    ? null
                    : normalizeStatementPeriod(statementPeriod, null);
            transactions = repository.findByFilters(normalizedPeriod, normalize(account), recurringMonthly);
        }

        return new IncomeTransactionList(transactions);
    }

    @Transactional
    public IncomeTransactionList getTransactionsByDateRange(LocalDate startDate,
                                                            LocalDate endDate,
                                                            String account,
                                                            Boolean recurringMonthly,
                                                            String transactionId) {
        logger.info("getIncomeTransactionsByDateRange entered. transactionId={}, startDate={}, endDate={}, account={}, recurringMonthly={}",
                transactionId, startDate, endDate, account, recurringMonthly);

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate/endDate are required and must form a valid inclusive range");
        }

        List<IncomeTransaction> transactions = repository.findByDateRangeFilters(startDate, endDate, normalize(account), recurringMonthly);
        return new IncomeTransactionList(transactions);
    }

    private void validateRecurringFields(IncomeTransaction transaction) {
        Boolean recurring = transaction.getRecurringMonthly();
        if (!Boolean.TRUE.equals(recurring)) {
            transaction.setRecurrenceStartMonth(null);
            transaction.setRecurrenceEndMonth(null);
            return;
        }

        String startMonth = normalizeMonthTag(transaction.getRecurrenceStartMonth());
        if (startMonth == null) {
            startMonth = transaction.getTransactionDate().withDayOfMonth(1).toString().substring(0, 7);
        }

        String endMonth = normalizeMonthTag(transaction.getRecurrenceEndMonth());
        if (endMonth != null && endMonth.compareTo(startMonth) < 0) {
            throw new IllegalArgumentException("recurrenceEndMonth must be on or after recurrenceStartMonth");
        }

        transaction.setRecurrenceStartMonth(startMonth);
        transaction.setRecurrenceEndMonth(endMonth);
    }

    private String normalizeStatementPeriod(String statementPeriod, LocalDate transactionDate) {
        if (statementPeriod != null && !statementPeriod.isBlank()) {
            return statementPeriod.trim().toUpperCase(Locale.ENGLISH);
        }
        LocalDate sourceDate = transactionDate != null ? transactionDate : LocalDate.now();
        return sourceDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase(Locale.ENGLISH)
                + sourceDate.getYear();
    }

    private String normalizeMonthTag(String monthTag) {
        if (monthTag == null || monthTag.isBlank()) {
            return null;
        }
        String normalized = monthTag.trim();
        try {
            YearMonth parsed = YearMonth.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM"));
            return parsed.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Recurring month fields must use YYYY-MM format");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ENGLISH);
    }
}
