DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_transaction_criticality_ids $$

CREATE PROCEDURE migrate_transaction_criticality_ids()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'budget_transactions'
          AND column_name = 'criticality_id'
    ) THEN
        ALTER TABLE budget_transactions
            ADD COLUMN criticality_id BIGINT NULL AFTER criticality;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'projected_transactions'
          AND column_name = 'criticality_id'
    ) THEN
        ALTER TABLE projected_transactions
            ADD COLUMN criticality_id BIGINT NULL AFTER criticality;
    END IF;

    UPDATE budget_transactions bt
    JOIN criticality c
      ON LOWER(TRIM(bt.criticality)) = LOWER(TRIM(c.name))
    SET bt.criticality_id = c.id
    WHERE bt.criticality_id IS NULL
      AND bt.criticality IS NOT NULL
      AND TRIM(bt.criticality) <> '';

    UPDATE projected_transactions pt
    JOIN criticality c
      ON LOWER(TRIM(pt.criticality)) = LOWER(TRIM(c.name))
    SET pt.criticality_id = c.id
    WHERE pt.criticality_id IS NULL
      AND pt.criticality IS NOT NULL
      AND TRIM(pt.criticality) <> '';

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'budget_transactions'
          AND index_name = 'idx_budget_criticality_id'
    ) THEN
        ALTER TABLE budget_transactions
            ADD INDEX idx_budget_criticality_id (criticality_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'projected_transactions'
          AND index_name = 'idx_projected_criticality_id'
    ) THEN
        ALTER TABLE projected_transactions
            ADD INDEX idx_projected_criticality_id (criticality_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'budget_transactions'
          AND constraint_name = 'fk_budget_transaction_criticality'
    ) THEN
        ALTER TABLE budget_transactions
            ADD CONSTRAINT fk_budget_transaction_criticality
                FOREIGN KEY (criticality_id) REFERENCES criticality(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.referential_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'projected_transactions'
          AND constraint_name = 'fk_projected_transaction_criticality'
    ) THEN
        ALTER TABLE projected_transactions
            ADD CONSTRAINT fk_projected_transaction_criticality
                FOREIGN KEY (criticality_id) REFERENCES criticality(id);
    END IF;
END $$

CALL migrate_transaction_criticality_ids() $$

DROP PROCEDURE IF EXISTS migrate_transaction_criticality_ids $$

DELIMITER ;
