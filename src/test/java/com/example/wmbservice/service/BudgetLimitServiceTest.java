package com.example.wmbservice.service;

import com.example.wmbservice.model.BudgetLimit;
import com.example.wmbservice.repository.BudgetLimitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetLimitServiceTest {

    @Mock
    private BudgetLimitRepository repository;

    @InjectMocks
    private BudgetLimitService service;

    @Test
    void upsert_createNewLimit_persistsAllThreeLimits() {
        when(repository.findByAccountAndStatementPeriod("Josh", "")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            BudgetLimit saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        BudgetLimit result = service.upsert("Josh", "january2026",
                new BigDecimal("300.00"), new BigDecimal("200.00"), new BigDecimal("500.00"), "tx-1");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAccount()).isEqualTo("Josh");
        assertThat(result.getStatementPeriod()).isEqualTo("");
        assertThat(result.getEssentialLimit()).isEqualByComparingTo("300.00");
        assertThat(result.getNonessentialLimit()).isEqualByComparingTo("200.00");
        assertThat(result.getTotalLimit()).isEqualByComparingTo("500.00");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(repository).save(any(BudgetLimit.class));
    }

    @Test
    void upsert_updateExistingLimit_overwritesValues() {
        BudgetLimit existing = budgetLimit("Josh", "", "400.00", "100.00", "500.00");
        existing.setId(7L);
        when(repository.findByAccountAndStatementPeriod("Josh", "")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BudgetLimit result = service.upsert("Josh", "february2026",
                new BigDecimal("250.00"), new BigDecimal("150.00"), new BigDecimal("400.00"), "tx-2");

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getEssentialLimit()).isEqualByComparingTo("250.00");
        assertThat(result.getNonessentialLimit()).isEqualByComparingTo("150.00");
        assertThat(result.getTotalLimit()).isEqualByComparingTo("400.00");
        verify(repository).save(existing);
    }

    @Test
    void upsert_partialLimits_persistsNullsForUnsetColumns() {
        when(repository.findByAccountAndStatementPeriod("Josh", "")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BudgetLimit result = service.upsert("Josh", "march2026",
                null, null, new BigDecimal("600.00"), "tx-3");

        assertThat(result.getEssentialLimit()).isNull();
        assertThat(result.getNonessentialLimit()).isNull();
        assertThat(result.getTotalLimit()).isEqualByComparingTo("600.00");
    }

    @Test
    void upsert_negativeLimitRejected() {
        assertThatThrownBy(() -> service.upsert("Josh", "april2026",
                new BigDecimal("-1.00"), null, null, "tx-4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("essentialLimit")
                .hasMessageContaining(">= 0");
    }

    @Test
    void upsert_blankAccountRejected() {
        assertThatThrownBy(() -> service.upsert("  ", "may2026",
                null, null, null, "tx-5"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("account");
    }

    @Test
    void upsert_ignoresStatementPeriod_andStoresBlank() {
        when(repository.findByAccountAndStatementPeriod("Josh", "")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BudgetLimit result = service.upsert("Josh", "june2026", null, null, null, "tx-7");

        assertThat(result.getStatementPeriod()).isEqualTo("");
        verify(repository).findByAccountAndStatementPeriod("Josh", "");
    }

    @Test
    void findByUserAndPeriod_returnsEmpty_whenNoneExists() {
        when(repository.findByAccountAndStatementPeriod("Josh", "JULY2026")).thenReturn(Optional.empty());

        Optional<BudgetLimit> result = service.findByAccountAndPeriod("Josh", "july2026");

        assertThat(result).isEmpty();
    }

    @Test
    void findByAccountAndPeriod_abbreviatedPeriodRejected() {
        assertThatThrownBy(() -> service.findByAccountAndPeriod("Josh", "JUN2026"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FULL_MONTHYYYY");
    }

    @Test
    void findByPeriod_ignoresProvidedPeriod_returnsAllUsers() {
        BudgetLimit a = budgetLimit("Alice", "", "100.00", null, "100.00");
        BudgetLimit b = budgetLimit("Bob", "", "200.00", "50.00", "250.00");
        when(repository.findAll()).thenReturn(List.of(a, b));

        List<BudgetLimit> result = service.findByPeriod();

        assertThat(result).hasSize(2)
                .extracting(BudgetLimit::getAccount)
                .containsExactly("Alice", "Bob");
    }

    @Test
    void findByPeriod_blankReturnsAllLimits() {
        BudgetLimit a = budgetLimit("Alice", "", "100.00", null, "100.00");
        BudgetLimit b = budgetLimit("Bob", "", "200.00", "50.00", "250.00");
        when(repository.findAll()).thenReturn(List.of(a, b));

        List<BudgetLimit> result = service.findByPeriod();

        assertThat(result).hasSize(2)
                .extracting(BudgetLimit::getAccount)
                .containsExactly("Alice", "Bob");
    }

    private static BudgetLimit budgetLimit(String account, String period,
                                           String essential, String nonessential, String total) {
        BudgetLimit bl = new BudgetLimit();
        bl.setAccount(account);
        bl.setStatementPeriod(period);
        bl.setEssentialLimit(essential != null ? new BigDecimal(essential) : null);
        bl.setNonessentialLimit(nonessential != null ? new BigDecimal(nonessential) : null);
        bl.setTotalLimit(total != null ? new BigDecimal(total) : null);
        return bl;
    }
}
