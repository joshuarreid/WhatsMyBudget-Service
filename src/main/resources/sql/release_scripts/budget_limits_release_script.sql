DROP PROCEDURE IF EXISTS migrate_budget_limits;
DROP TABLE IF EXISTS budget_limits;

CREATE TABLE budget_limits (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    account             VARCHAR(64)    NOT NULL,
    statement_period    VARCHAR(32)    NOT NULL,
    essential_limit     DECIMAL(12,2)  NULL,
    nonessential_limit  DECIMAL(12,2)  NULL,
    total_limit         DECIMAL(12,2)  NULL,
    created_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_budget_limits_account_period (account, statement_period),
    INDEX idx_budget_limits_account (account),
    INDEX idx_budget_limits_statement_period (statement_period)
);