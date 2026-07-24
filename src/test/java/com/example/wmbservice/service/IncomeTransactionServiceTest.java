package com.example.wmbservice.service;

import com.example.wmbservice.model.IncomeTransaction;
import com.example.wmbservice.model.IncomeTransactionList;
import com.example.wmbservice.repository.IncomeTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncomeTransactionServiceTest {

    @Mock
    private IncomeTransactionRepository repository;

    @InjectMocks
    private IncomeTransactionService service;

    @Test
    void getTransactions_withoutFilters_returnsListWithCountAndTotal() {
        IncomeTransaction first = sampleIncome("Paycheck", "1000.00", LocalDate.of(2026, 5, 10));
        IncomeTransaction second = sampleIncome("Bonus", "250.00", LocalDate.of(2026, 5, 20));
        when(repository.findAll()).thenReturn(List.of(first, second));

        IncomeTransactionList result = service.getTransactions(null, null, null, "tx-svc-list");

        assertThat(result.getCount()).isEqualTo(2);
        assertThat(result.getTotal()).isEqualByComparingTo("1250.00");
        assertThat(result.getTransactions()).hasSize(2);
    }

    @Test
    void createTransaction_validPayload_persistsAndReturnsSavedEntity() {
        IncomeTransaction request = sampleIncome("Paycheck", "2500.00", LocalDate.of(2026, 5, 1));
        request.setStatementPeriod("may2026");
        request.setAccount("Josh");

        IncomeTransaction saved = sampleIncome("Paycheck", "2500.00", LocalDate.of(2026, 5, 1));
        saved.setId(1L);
        saved.setStatementPeriod("MAY2026");

        when(repository.save(any(IncomeTransaction.class))).thenReturn(saved);

        IncomeTransaction result = service.createTransaction(request, "tx-svc-create");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Paycheck");
        assertThat(result.getStatementPeriod()).isEqualTo("MAY2026");
        verify(repository).save(any(IncomeTransaction.class));
    }

    @Test
    void createTransaction_withClientSuppliedId_rejectsRequest() {
        IncomeTransaction request = sampleIncome("Paycheck", "2500.00", LocalDate.of(2026, 5, 1));
        request.setId(42L);

        assertThatThrownBy(() -> service.createTransaction(request, "tx-svc-create-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id must not be provided");
    }

    @Test
    void createTransaction_withInvalidRecurringMonth_rejectsRequest() {
        IncomeTransaction request = sampleIncome("Paycheck", "2500.00", LocalDate.of(2026, 5, 1));
        request.setRecurringMonthly(true);
        request.setRecurrenceStartMonth("2026-13");

        assertThatThrownBy(() -> service.createTransaction(request, "tx-svc-recurring"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("YYYY-MM");
    }

    private static IncomeTransaction sampleIncome(String description, String amount, LocalDate transactionDate) {
        IncomeTransaction tx = new IncomeTransaction();
        tx.setDescription(description);
        tx.setAmount(new BigDecimal(amount));
        tx.setTransactionDate(transactionDate);
        tx.setAccount("josh");
        tx.setRecurringMonthly(false);
        return tx;
    }
}
