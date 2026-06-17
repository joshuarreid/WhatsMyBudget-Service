package com.example.wmbservice.repository;

import com.example.wmbservice.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Account lookups.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find an account by name, case-insensitive.
     * Used for account name validation and ID resolution on writes.
     */
    Optional<Account> findByAccountNameIgnoreCase(String accountName);
}

