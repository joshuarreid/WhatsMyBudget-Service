-- MySQL 8 Schema for Statement-Based Budgeting Application (No ENUM columns, all VARCHAR, semicolons after each statement)

CREATE TABLE criticality (
    id BIGINT PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE,
    INDEX idx_criticality_name (name)
);

INSERT INTO criticality (id, name) VALUES
    (1, 'Essential'),
    (2, 'Nonessential'),
    (3, 'Planned');

CREATE TABLE budget_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category VARCHAR(128) NOT NULL,
    criticality VARCHAR(32) NOT NULL,
    criticality_id BIGINT,
    transaction_date DATE NOT NULL,
    account VARCHAR(32) NOT NULL,
    status VARCHAR(64),
    created_time DATETIME,
    payment_method VARCHAR(64) NOT NULL,
    statement_period VARCHAR(32) NOT NULL,
    row_hash VARCHAR(64) NULL,
    INDEX idx_budget_statement_period (statement_period),
    INDEX idx_budget_transaction_date (transaction_date),
    INDEX idx_budget_account (account),
    INDEX idx_budget_payment_method (payment_method),
    INDEX idx_budget_category (category),
    INDEX idx_budget_row_hash (row_hash),
    INDEX idx_budget_criticality_id (criticality_id),
    CONSTRAINT fk_budget_transaction_criticality FOREIGN KEY (criticality_id) REFERENCES criticality(id)
);

CREATE TABLE projected_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category VARCHAR(128) NOT NULL,
    criticality VARCHAR(32) NOT NULL,
    criticality_id BIGINT,
    transaction_date DATE,
    account VARCHAR(32) NOT NULL,
    status VARCHAR(64),
    created_time DATETIME,
    payment_method VARCHAR(64) NOT NULL,
    statement_period VARCHAR(32) NOT NULL,
    INDEX idx_projected_statement_period (statement_period),
    INDEX idx_projected_transaction_date (transaction_date),
    INDEX idx_projected_account (account),
    INDEX idx_projected_payment_method (payment_method),
    INDEX idx_projected_category (category),
    INDEX idx_projected_criticality_id (criticality_id),
    CONSTRAINT fk_projected_transaction_criticality FOREIGN KEY (criticality_id) REFERENCES criticality(id)
);

CREATE TABLE statement_periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_name VARCHAR(32) NOT NULL UNIQUE,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_statement_periods_period_name (period_name),
    INDEX idx_statement_periods_start_date (start_date),
    INDEX idx_statement_periods_end_date (end_date)
);

CREATE TABLE local_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cache_key VARCHAR(128) NOT NULL UNIQUE,
    cache_value TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE archived_statement_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    statement_period VARCHAR(32) NOT NULL,
    payment_method VARCHAR(64) NOT NULL,
    user_name VARCHAR(64) NOT NULL,
    amount_owed DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_archived_summary_statement_period (statement_period),
    INDEX idx_archived_summary_payment_method (payment_method),
    INDEX idx_archived_summary_user (user_name),
    UNIQUE KEY uniq_archived_summary_period_payment_user (statement_period, payment_method, user_name)
);

CREATE TABLE archived_statement_category_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    statement_period VARCHAR(32) NOT NULL,
    payment_method VARCHAR(64) NOT NULL,
    account VARCHAR(32) NOT NULL,
    category VARCHAR(128) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    INDEX idx_archived_category_period_payment_account (statement_period, payment_method, account),
    INDEX idx_archived_category_category (category)
);

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

CREATE TABLE workspace_backups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    backup_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    workspace_json LONGTEXT NOT NULL,
    budget_transactions_hash VARCHAR(64),
    projections_hash VARCHAR(64),
    local_cache_hash VARCHAR(64),
    version VARCHAR(16),
    INDEX idx_workspace_backups_backup_time (backup_time)
);
