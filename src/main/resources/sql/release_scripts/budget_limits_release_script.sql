DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_budget_limits $$

CREATE PROCEDURE migrate_budget_limits()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'budget_limits'
    ) THEN
        CREATE TABLE budget_limits (
            id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
            user_name           VARCHAR(64)    NOT NULL,
            statement_period    VARCHAR(32)    NOT NULL,
            essential_limit     DECIMAL(12,2)  NULL,
            nonessential_limit  DECIMAL(12,2)  NULL,
            total_limit         DECIMAL(12,2)  NULL,
            created_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
            updated_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            UNIQUE KEY uniq_budget_limits_user_period (user_name, statement_period),
            INDEX idx_budget_limits_user_name (user_name),
            INDEX idx_budget_limits_statement_period (statement_period)
        );
    END IF;
END $$

CALL migrate_budget_limits() $$

DROP PROCEDURE IF EXISTS migrate_budget_limits $$

DELIMITER ;


