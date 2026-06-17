package com.example.wmbservice.service;

import com.example.wmbservice.model.Account;
import com.example.wmbservice.model.ProjectedTransaction;
import com.example.wmbservice.repository.ProjectedTransactionRepository;
import com.example.wmbservice.repository.StatementPeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectedTransactionServiceAccountResolutionTest {

    @Mock ProjectedTransactionRepository repository;
    @Mock StatementPeriodRepository statementPeriodRepository;
    @Mock CriticalityService criticalityService;
    @Mock AccountService accountService;

    private ProjectedTransactionService service;

    @BeforeEach
    void setUp() {
        service = new ProjectedTransactionService(
                repository, statementPeriodRepository, criticalityService, accountService);
    }

    @Test
    void createTransaction_knownAccount_setsAccountIdAndNormalizesName() {
        Account account = new Account(3L, "anna");
        when(accountService.resolveByName("Anna")).thenReturn(account);
        when(statementPeriodRepository.findByPeriodName("JUNE2026"))
                .thenReturn(Optional.of(new com.example.wmbservice.model.StatementPeriod()));
        doNothing().when(criticalityService).normalize(any(ProjectedTransaction.class));

        ProjectedTransaction tx = validTransaction("Anna");
        ProjectedTransaction saved = validTransaction("anna");
        saved.setAccountId(3L);
        when(repository.findByBusinessKey(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(saved);

        service.createTransaction(tx, "tx-1");

        verify(accountService).resolveByName("Anna");
        assertThat(tx.getAccount()).isEqualTo("anna");
        assertThat(tx.getAccountId()).isEqualTo(3L);
    }

    @Test
    void createTransaction_unknownAccount_throwsUnknownAccountException() {
        when(accountService.resolveByName("ghost"))
                .thenThrow(new AccountService.UnknownAccountException("ghost"));
        when(statementPeriodRepository.findByPeriodName("JUNE2026"))
                .thenReturn(Optional.of(new com.example.wmbservice.model.StatementPeriod()));
        doNothing().when(criticalityService).normalize(any(ProjectedTransaction.class));

        ProjectedTransaction tx = validTransaction("ghost");

        assertThatThrownBy(() -> service.createTransaction(tx, "tx-2"))
                .isInstanceOf(AccountService.UnknownAccountException.class)
                .hasMessageContaining("ghost");

        verify(repository, never()).save(any());
    }

    @Test
    void updateTransaction_unknownAccount_throwsUnknownAccountException() {
        ProjectedTransaction existing = validTransaction("anna");
        existing.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(criticalityService).normalize(any(ProjectedTransaction.class));
        when(statementPeriodRepository.findByPeriodName("JUNE2026"))
                .thenReturn(Optional.of(new com.example.wmbservice.model.StatementPeriod()));

        ProjectedTransaction updated = validTransaction("phantom");
        when(accountService.resolveByName("phantom"))
                .thenThrow(new AccountService.UnknownAccountException("phantom"));

        assertThatThrownBy(() -> service.updateTransaction(1L, updated, "tx-3"))
                .isInstanceOf(AccountService.UnknownAccountException.class)
                .hasMessageContaining("phantom");

        verify(repository, never()).save(any());
    }

    private ProjectedTransaction validTransaction(String accountName) {
        ProjectedTransaction tx = new ProjectedTransaction();
        tx.setName("Projected");
        tx.setAmount(new BigDecimal("25.00"));
        tx.setCategory("utilities");
        tx.setCriticality("Essential");
        tx.setCriticalityId(1L);
        tx.setTransactionDate(LocalDate.of(2026, 6, 15));
        tx.setAccount(accountName);
        tx.setPaymentMethod("visa");
        tx.setStatementPeriod("JUNE2026");
        tx.setCreatedTime(LocalDateTime.now());
        return tx;
    }
}

