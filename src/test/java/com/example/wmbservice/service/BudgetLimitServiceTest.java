package com.example.wmbservice.service;

import com.example.wmbservice.model.BudgetLimit;
import com.example.wmbservice.repository.BudgetLimitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    // --- upsert: create ---

    @Test
    void upsert_createNewLimit_persistsAllThreeLimits() {
        when(repository.findByUserNameAndStatementPeriod("Josh", "JAN2026")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> {
            BudgetLimit saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        BudgetLimit result = service.upsert("Josh", "jan2026",
                new BigDecimal("300.00"), new BigDecimal("200.00"), new BigDecimal("500.00"), "tx-1");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserName()).isEqualTo("Josh");
        assertThat(result.getStatementPeriod()).isEqualTo("JAN2026");
        assertThat(result.getEssentialLimit()).isEqualByComparingTo("300.00");
        assertThat(result.getNonessentialLimit()).isEqualByComparingTo("200.00");
        assertThat(result.getTotalLimit()).isEqualByComparingTo("500.00");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(repository).save(any(BudgetLimit.class));
    }

    @Test
    void upsert_updateExistingLimit_overwritesValues() {
        BudgetLimit existing = budgetLimit("Josh", "FEB2026", "400.00", "100.00", "500.00");
        existing.setId(7L);
        when(repository.findByUserNameAndStatementPeriod("Josh", "FEB2026")).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BudgetLimit result = service.upsert("Josh", "FEB2026",
                new BigDecimal("250.00"), new BigDecimal("150.00"), new BigDecimal("400.00"), "tx-2");

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getEssentialLimit()).isEqualByComparingTo("250.00");
        assertThat(result.getNonessentialLimit()).isEqualByComparingTo("150.00");
        assertThat(result.getTotalLimit()).isEqualByComparingTo("400.00");
        // createdAt not mutated on update
        verify(repository).save(existing);
    }

    @Test
    void upsert_partialLimits_persistsNullsForUnsetColumns() {
        when(repository.findByUserNameAndStatementPeriod("Josh", "MAR2026")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BudgetLimit result = service.upsert("Josh", "MAR2026",
                null, null, new BigDecimal("600.00"), "tx-3");

        assertThat(result.getEssentialLimit()).isNull();
        assertThat(result.getNonessentialLimit()).isNull();
        assertThat(result.getTotalLimit()).isEqualByComparingTo("600.00");
    }

    // --- upsert: validation ---

    @Test
    void upsert_negativeLimitRejected() {
        assertThatThrownBy(() -> service.upsert("Josh", "APR2026",
                new BigDecimal("-1.00"), null, null, "tx-4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("essentialLimit")
                .hasMessageContaining(">= 0");
    }

    @Test
    void upsert_blankUserNameRejected() {
        assertThatThrownBy(() -> service.upsert("  ", "MAY2026",
                null, null, null, "tx-5"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userName");
    }

    @Test
    void upsert_blankPeriodRejected() {
        assertThatThrownBy(() -> service.upsert("Josh", "",
                null, null, null, "tx-6"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statementPeriod");
    }

    @Test
    void upsert_periodNormalisedToUppercase() {
        when(repository.findByUserNameAndStatementPeriod(eq("Josh"), eq("JUN2026"))).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BudgetLimit result = service.upsert("Josh", "jun2026", null, null, null, "tx-7");

        assertThat(result.getStatementPeriod()).isEqualTo("JUN2026");
        verify(repository).findByUserNameAndStatementPeriod("Josh", "JUN2026");
    }

    // --- query methods ---

    @Test
    void findByUserAndPeriod_returnsEmpty_whenNoneExists() {
        when(repository.findByUserNameAndStatementPeriod("Josh", "JUL2026")).thenReturn(Optional.empty());

        Optional<BudgetLimit> result = service.findByUserAndPeriod("Josh", "jul2026");

        assertThat(result).isEmpty();
    }

    @Test
    void findByPeriod_returnsAllUsersForPeriod() {
        BudgetLimit a = budgetLimit("Alice", "AUG2026", "100.00", null, "100.00");
        BudgetLimit b = budgetLimit("Bob", "AUG2026", "200.00", "50.00", "250.00");
        when(repository.findByStatementPeriod("AUG2026")).thenReturn(List.of(a, b));

        List<BudgetLimit> result = service.findByPeriod("aug2026");

        assertThat(result).hasSize(2)
                .extracting(BudgetLimit::getUserName)
                .containsExactly("Alice", "Bob");
    }

    // --- helpers ---

    private static BudgetLimit budgetLimit(String userName, String period,
                                           String essential, String nonessential, String total) {
        BudgetLimit bl = new BudgetLimit();
        bl.setUserName(userName);
        bl.setStatementPeriod(period);
        bl.setEssentialLimit(essential != null ? new BigDecimal(essential) : null);
        bl.setNonessentialLimit(nonessential != null ? new BigDecimal(nonessential) : null);
        bl.setTotalLimit(total != null ? new BigDecimal(total) : null);
        return bl;
    }
}

