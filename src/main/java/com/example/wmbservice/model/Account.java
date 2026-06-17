package com.example.wmbservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing a financial account, mapped to the 'accounts' table.
 * Accounts are the canonical source for account names used across transactions.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_accounts_name", columnList = "account_name")
        }
)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_name", nullable = false, unique = true, length = 32)
    private String accountName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Account(Long id, String accountName) {
        this.id = id;
        this.accountName = accountName;
    }
}

