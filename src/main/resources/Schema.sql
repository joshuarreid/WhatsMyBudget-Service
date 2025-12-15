-- MySQL 8 Schema for Statement-Based Budgeting Application (No ENUM columns, all VARCHAR, semicolons after each statement)

CREATE TABLE budget_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category VARCHAR(128) NOT NULL,
    criticality VARCHAR(32) NOT NULL,
    transaction_date DATE NOT NULL,
    account VARCHAR(32) NOT NULL,
    status VARCHAR(64),
    created_time DATETIME,
    payment_method VARCHAR(64) NOT NULL,
    statement_period VARCHAR(32) NOT NULL,
    row_hash VARCHAR(64) NULL,
    INDEX idx_statement_period (statement_period),
    INDEX idx_account (account),
    INDEX idx_payment_method (payment_method),
    INDEX idx_category (category),
    INDEX idx_row_hash (row_hash)
);

CREATE TABLE projected_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category VARCHAR(128) NOT NULL,
    criticality VARCHAR(32) NOT NULL,
    transaction_date DATE,
    account VARCHAR(32) NOT NULL,
    status VARCHAR(64),
    created_time DATETIME,
    payment_method VARCHAR(64) NOT NULL,
    statement_period VARCHAR(32) NOT NULL,
    INDEX idx_statement_period (statement_period),
    INDEX idx_account (account),
    INDEX idx_payment_method (payment_method),
    INDEX idx_category (category)
);

CREATE TABLE statement_periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_name VARCHAR(32) NOT NULL UNIQUE,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    INDEX idx_statement_period (statement_period),
    INDEX idx_payment_method (payment_method),
    INDEX idx_user (user_name),
    UNIQUE KEY uniq_summary (statement_period, payment_method, user_name)
);

CREATE TABLE archived_statement_category_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    statement_period VARCHAR(32) NOT NULL,
    payment_method VARCHAR(64) NOT NULL,
    account VARCHAR(32) NOT NULL,
    category VARCHAR(128) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    INDEX idx_period_card_account (statement_period, payment_method, account),
    INDEX idx_category (category)
);

CREATE TABLE workspace_backups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    backup_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    workspace_json LONGTEXT NOT NULL,
    budget_transactions_hash VARCHAR(64),
    projections_hash VARCHAR(64),
    local_cache_hash VARCHAR(64),
    version VARCHAR(16),
    INDEX idx_backup_time (backup_time)
);