-- Release script: accounts table + account_id dual-write columns
-- ADR: docs/decisions/ADR-003-accounts-table-and-name-validation.md
--
-- What this script does (all steps are idempotent):
--   1. Create the `accounts` table if it does not exist.
--   2. Seed known account names into `accounts` if the table is empty.
--   3. Add nullable `account_id` column to `budget_transactions` if it does not exist.
--   4. Add nullable `account_id` column to `projected_transactions` if it does not exist.
--   5. Add indexes on both new `account_id` columns if they do not exist.
--   6. Backfill `account_id` on `budget_transactions` by matching `account` name (case-insensitive).
--   7. Backfill `account_id` on `projected_transactions` by matching `account` name (case-insensitive).
--
-- Safe to run multiple times. Does NOT enforce NOT NULL or FK yet — that is a follow-up
-- step once all rows have been backfilled and verified.
--
-- Verification query (run after this script):
--   SELECT COUNT(*) FROM budget_transactions WHERE account_id IS NULL;
--   SELECT COUNT(*) FROM projected_transactions WHERE account_id IS NULL;
-- Both should return 0 before adding NOT NULL constraints.

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_accounts_and_account_id $$

CREATE PROCEDURE migrate_accounts_and_account_id()
BEGIN

    -- -------------------------------------------------------------------------
    -- Step 1: Create accounts table if it does not exist
    -- -------------------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'accounts'
    ) THEN
        CREATE TABLE accounts (
            id          BIGINT AUTO_INCREMENT PRIMARY KEY,
            account_name VARCHAR(32) NOT NULL UNIQUE,
            created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_accounts_name (account_name)
        );
    END IF;

    -- -------------------------------------------------------------------------
    -- Step 2: Seed known accounts if the table is empty
    -- Adjust this list to match the distinct account values in your database.
    -- Run: SELECT DISTINCT account FROM budget_transactions; to discover them.
    -- -------------------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM accounts LIMIT 1) THEN
        INSERT INTO accounts (account_name) VALUES
            ('josh'),
            ('anna'),
            ('joint');
    END IF;

    -- -------------------------------------------------------------------------
    -- Step 3: Add account_id to budget_transactions if it does not exist
    -- -------------------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'budget_transactions'
          AND column_name = 'account_id'
    ) THEN
        ALTER TABLE budget_transactions
            ADD COLUMN account_id BIGINT NULL;
    END IF;

    -- -------------------------------------------------------------------------
    -- Step 4: Add account_id to projected_transactions if it does not exist
    -- -------------------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'projected_transactions'
          AND column_name = 'account_id'
    ) THEN
        ALTER TABLE projected_transactions
            ADD COLUMN account_id BIGINT NULL;
    END IF;

    -- -------------------------------------------------------------------------
    -- Step 5: Add indexes on account_id columns if they do not exist
    -- -------------------------------------------------------------------------
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'budget_transactions'
          AND index_name = 'idx_budget_account_id'
    ) THEN
        ALTER TABLE budget_transactions
            ADD INDEX idx_budget_account_id (account_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'projected_transactions'
          AND index_name = 'idx_projected_account_id'
    ) THEN
        ALTER TABLE projected_transactions
            ADD INDEX idx_projected_account_id (account_id);
    END IF;

    -- -------------------------------------------------------------------------
    -- Step 6: Backfill account_id on budget_transactions (case-insensitive match)
    -- Only updates rows where account_id is currently NULL.
    -- -------------------------------------------------------------------------
    UPDATE budget_transactions bt
    JOIN accounts a ON LOWER(TRIM(bt.account)) = LOWER(TRIM(a.account_name))
    SET bt.account_id = a.id
    WHERE bt.account_id IS NULL;

    -- -------------------------------------------------------------------------
    -- Step 7: Backfill account_id on projected_transactions (case-insensitive match)
    -- Only updates rows where account_id is currently NULL.
    -- -------------------------------------------------------------------------
    UPDATE projected_transactions pt
    JOIN accounts a ON LOWER(TRIM(pt.account)) = LOWER(TRIM(a.account_name))
    SET pt.account_id = a.id
    WHERE pt.account_id IS NULL;

END $$

CALL migrate_accounts_and_account_id() $$

DROP PROCEDURE IF EXISTS migrate_accounts_and_account_id $$

DELIMITER ;

