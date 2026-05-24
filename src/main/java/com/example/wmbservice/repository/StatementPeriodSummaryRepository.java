package com.example.wmbservice.repository;

import com.example.wmbservice.model.StatementPeriodSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatementPeriodSummaryRepository extends JpaRepository<StatementPeriodSummary, Long> {
    Optional<StatementPeriodSummary> findByStatementPeriod(String statementPeriod);
}

