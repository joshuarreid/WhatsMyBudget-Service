package com.example.wmbservice.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class IncomeTransactionList {
    private List<IncomeTransaction> transactions;
    private int count;
    private BigDecimal total;

    public IncomeTransactionList(List<IncomeTransaction> transactions) {
        if (transactions == null) {
            this.transactions = Collections.emptyList();
            this.count = 0;
            this.total = BigDecimal.ZERO;
            return;
        }

        this.transactions = List.copyOf(transactions);
        this.count = transactions.size();
        this.total = transactions.stream()
                .filter(Objects::nonNull)
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public IncomeTransactionList() {
        this(Collections.emptyList());
    }
}
