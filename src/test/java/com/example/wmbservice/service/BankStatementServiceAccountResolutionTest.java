package com.example.wmbservice.service;

import com.example.wmbservice.model.Bank;
import com.example.wmbservice.model.BulkImportResult;
import com.example.wmbservice.repository.BudgetTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankStatementServiceAccountResolutionTest {

    @Mock BudgetTransactionRepository repository;
    @Mock AccountService accountService;

    private BankStatementService bankStatementService;

    @BeforeEach
    void setUp() {
        bankStatementService = new BankStatementService(repository, accountService);
    }

    @Test
    void importCreditCardStatement_unknownAccount_returnsBulkImportErrorImmediately() {
        when(accountService.resolveByName("unknown"))
                .thenThrow(new AccountService.UnknownAccountException("unknown"));

        MultipartFile dummyFile = new MockMultipartFile("file", "test.csv", "text/csv",
                "Transaction Date,Post Date,Description,Category,Type,Amount\n".getBytes());

        BulkImportResult result = bankStatementService.importCreditCardStatement(
                dummyFile, Bank.CHASE, "JUNE2026", "unknown", "visa", "tx-1");

        assertThat(result.getInsertedCount()).isZero();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).get("message").toString()).contains("unknown");
        verify(repository, never()).save(any());
    }

    @Test
    void importCreditCardStatement_knownAccount_proceedsToParseFile() {
        com.example.wmbservice.model.Account account = new com.example.wmbservice.model.Account(1L, "josh");
        when(accountService.resolveByName("josh")).thenReturn(account);

        // Empty CSV (no data rows after header) — should produce zero inserts, not an error from account resolution
        MultipartFile emptyFile = new MockMultipartFile("file", "test.csv", "text/csv",
                "Transaction Date,Post Date,Description,Category,Type,Amount\n".getBytes());

        BulkImportResult result = bankStatementService.importCreditCardStatement(
                emptyFile, Bank.CHASE, "JUNE2026", "josh", "visa", "tx-2");

        assertThat(result.getInsertedCount()).isZero();
        // No account-resolution errors
        assertThat(result.getErrors().stream()
                .filter(e -> e.get("message") != null && e.get("message").toString().contains("Unknown account"))
                .count()).isZero();
    }
}

