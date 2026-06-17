package com.example.wmbservice.service;

import com.example.wmbservice.model.Account;
import com.example.wmbservice.model.BudgetTransaction;
import com.example.wmbservice.repository.BudgetTransactionRepository;
import com.example.wmbservice.repository.StatementPeriodRepository;
import com.example.wmbservice.util.BudgetTransactionCsvImporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetTransactionServiceAccountResolutionTest {

    @Mock BudgetTransactionRepository repository;
    @Mock BudgetTransactionCsvImporter csvImporter;
    @Mock StatementPeriodRepository statementPeriodRepository;
    @Mock BankStatementService bankStatementService;
    @Mock CriticalityService criticalityService;
    @Mock AccountService accountService;

    private BudgetTransactionService service;

    @BeforeEach
    void setUp() {
        service = new BudgetTransactionService(
                repository, csvImporter, statementPeriodRepository,
                bankStatementService, criticalityService, accountService);
    }

    @Test
    void createTransaction_knownAccount_setsAccountIdAndNormalizesName() {
        Account account = new Account(5L, "josh");
        when(accountService.resolveByName("Josh")).thenReturn(account);
        when(statementPeriodRepository.findByPeriodName("JUNE2026")).thenReturn(Optional.of(new com.example.wmbservice.model.StatementPeriod()));
        doNothing().when(criticalityService).normalize(any(BudgetTransaction.class));

        BudgetTransaction tx = validTransaction("Josh");
        BudgetTransaction saved = validTransaction("josh");
        saved.setAccountId(5L);
        when(repository.findByRowHashAndStatementPeriod(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(saved);

        BudgetTransaction result = service.createTransaction(tx, "tx-1");

        verify(accountService).resolveByName("Josh");
        // account name should be normalized to lowercase
        assertThat(tx.getAccount()).isEqualTo("josh");
        assertThat(tx.getAccountId()).isEqualTo(5L);
    }

    @Test
    void createTransaction_unknownAccount_throwsUnknownAccountException() {
        when(accountService.resolveByName("mystery"))
                .thenThrow(new AccountService.UnknownAccountException("mystery"));
        when(statementPeriodRepository.findByPeriodName("JUNE2026")).thenReturn(Optional.of(new com.example.wmbservice.model.StatementPeriod()));
        doNothing().when(criticalityService).normalize(any(BudgetTransaction.class));

        BudgetTransaction tx = validTransaction("mystery");

        assertThatThrownBy(() -> service.createTransaction(tx, "tx-2"))
                .isInstanceOf(AccountService.UnknownAccountException.class)
                .hasMessageContaining("mystery");

        verify(repository, never()).save(any());
    }

    @Test
    void updateTransaction_unknownAccount_throwsUnknownAccountException() {
        BudgetTransaction existing = validTransaction("josh");
        existing.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(criticalityService).normalize(any(BudgetTransaction.class));
        when(statementPeriodRepository.findByPeriodName("JUNE2026")).thenReturn(Optional.of(new com.example.wmbservice.model.StatementPeriod()));

        BudgetTransaction updated = validTransaction("newaccount");
        when(accountService.resolveByName("newaccount"))
                .thenThrow(new AccountService.UnknownAccountException("newaccount"));

        assertThatThrownBy(() -> service.updateTransaction(1L, updated, "tx-3"))
                .isInstanceOf(AccountService.UnknownAccountException.class)
                .hasMessageContaining("newaccount");

        verify(repository, never()).save(any());
    }

    private BudgetTransaction validTransaction(String accountName) {
        BudgetTransaction tx = new BudgetTransaction();
        tx.setName("Test");
        tx.setAmount(new BigDecimal("10.00"));
        tx.setCategory("food");
        tx.setCriticality("Essential");
        tx.setCriticalityId(1L);
        tx.setTransactionDate(LocalDate.of(2026, 6, 1));
        tx.setAccount(accountName);
        tx.setPaymentMethod("visa");
        tx.setStatementPeriod("JUNE2026");
        return tx;
    }
}

