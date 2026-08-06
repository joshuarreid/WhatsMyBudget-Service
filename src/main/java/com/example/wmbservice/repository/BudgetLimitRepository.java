package com.example.wmbservice.repository;

import com.example.wmbservice.model.BudgetLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetLimitRepository extends JpaRepository<BudgetLimit, Long> {

    Optional<BudgetLimit> findByAccountAndStatementPeriod(String account, String statementPeriod);

    List<BudgetLimit> findByStatementPeriod(String statementPeriod);

    List<BudgetLimit> findByAccount(String account);
}

