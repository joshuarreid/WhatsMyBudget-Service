DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_statement_period_summary_indexes $$

CREATE PROCEDURE migrate_statement_period_summary_indexes()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'budget_transactions'
          AND index_name = 'idx_account'
    ) THEN
        ALTER TABLE budget_transactions
            RENAME INDEX idx_account TO idx_budget_account;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'budget_transactions'
          AND index_name = 'idx_category'
    ) THEN
        ALTER TABLE budget_transactions
            RENAME INDEX idx_category TO idx_budget_category;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'budget_transactions'
          AND index_name = 'idx_payment_method'
    ) THEN
        ALTER TABLE budget_transactions
            RENAME INDEX idx_payment_method TO idx_budget_payment_method;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'budget_transactions'
          AND index_name = 'idx_statement_period'
    ) THEN
        ALTER TABLE budget_transactions
            RENAME INDEX idx_statement_period TO idx_budget_statement_period;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'budget_transactions'
          AND index_name = 'idx_budget_transaction_date'
    ) THEN
        ALTER TABLE budget_transactions
            ADD INDEX idx_budget_transaction_date (transaction_date);
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'projected_transactions'
          AND index_name = 'idx_account'
    ) THEN
        ALTER TABLE projected_transactions
            RENAME INDEX idx_account TO idx_projected_account;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'projected_transactions'
          AND index_name = 'idx_category'
    ) THEN
        ALTER TABLE projected_transactions
            RENAME INDEX idx_category TO idx_projected_category;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'projected_transactions'
          AND index_name = 'idx_payment_method'
    ) THEN
        ALTER TABLE projected_transactions
            RENAME INDEX idx_payment_method TO idx_projected_payment_method;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'projected_transactions'
          AND index_name = 'idx_statement_period'
    ) THEN
        ALTER TABLE projected_transactions
            RENAME INDEX idx_statement_period TO idx_projected_statement_period;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'projected_transactions'
          AND index_name = 'idx_projected_transaction_date'
    ) THEN
        ALTER TABLE projected_transactions
            ADD INDEX idx_projected_transaction_date (transaction_date);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'statement_periods'
          AND index_name = 'idx_statement_periods_period_name'
    ) THEN
        ALTER TABLE statement_periods
            ADD INDEX idx_statement_periods_period_name (period_name);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'statement_periods'
          AND index_name = 'idx_statement_periods_start_date'
    ) THEN
        ALTER TABLE statement_periods
            ADD INDEX idx_statement_periods_start_date (start_date);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'statement_periods'
          AND index_name = 'idx_statement_periods_end_date'
    ) THEN
        ALTER TABLE statement_periods
            ADD INDEX idx_statement_periods_end_date (end_date);
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'archived_statement_summary'
          AND index_name = 'idx_payment_method'
    ) THEN
        ALTER TABLE archived_statement_summary
            RENAME INDEX idx_payment_method TO idx_archived_summary_payment_method;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'archived_statement_summary'
          AND index_name = 'idx_statement_period'
    ) THEN
        ALTER TABLE archived_statement_summary
            RENAME INDEX idx_statement_period TO idx_archived_summary_statement_period;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'archived_statement_summary'
          AND index_name = 'idx_user'
    ) THEN
        ALTER TABLE archived_statement_summary
            RENAME INDEX idx_user TO idx_archived_summary_user;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'archived_statement_summary'
          AND index_name = 'uniq_summary'
    ) THEN
        ALTER TABLE archived_statement_summary
            RENAME INDEX uniq_summary TO uniq_archived_summary_period_payment_user;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'archived_statement_category_summary'
          AND index_name = 'idx_category'
    ) THEN
        ALTER TABLE archived_statement_category_summary
            RENAME INDEX idx_category TO idx_archived_category_category;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'archived_statement_category_summary'
          AND index_name = 'idx_period_card_account'
    ) THEN
        ALTER TABLE archived_statement_category_summary
            RENAME INDEX idx_period_card_account TO idx_archived_category_period_payment_account;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'workspace_backups'
          AND index_name = 'idx_backup_time'
    ) THEN
        ALTER TABLE workspace_backups
            RENAME INDEX idx_backup_time TO idx_workspace_backups_backup_time;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'statement_period_summaries'
    ) THEN
        CREATE TABLE statement_period_summaries (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            statement_period VARCHAR(32) NOT NULL,
            period_start_date DATE,
            period_end_date DATE,
            total_amount DECIMAL(12,2) NOT NULL,
            transaction_count BIGINT NOT NULL,
            essential_amount DECIMAL(12,2) NOT NULL,
            essential_count BIGINT NOT NULL,
            nonessential_amount DECIMAL(12,2) NOT NULL,
            nonessential_count BIGINT NOT NULL,
            category_breakdown_json LONGTEXT,
            criticality_breakdown_json LONGTEXT,
            account_breakdown_json LONGTEXT,
            payment_method_breakdown_json LONGTEXT,
            outliers_json LONGTEXT,
            generated_at DATETIME NOT NULL,
            UNIQUE KEY uniq_statement_period_summary_period (statement_period),
            INDEX idx_statement_period_summary_start_date (period_start_date),
            INDEX idx_statement_period_summary_end_date (period_end_date)
        );
    END IF;
END $$

CALL migrate_statement_period_summary_indexes() $$

DROP PROCEDURE IF EXISTS migrate_statement_period_summary_indexes $$

DELIMITER ;