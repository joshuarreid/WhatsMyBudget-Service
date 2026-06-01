CREATE TABLE IF NOT EXISTS criticality (
    id BIGINT PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE,
    INDEX idx_criticality_name (name)
);

INSERT INTO criticality (id, name)
VALUES
    (1, 'Essential'),
    (2, 'Nonessential'),
    (3, 'Planned')
ON DUPLICATE KEY UPDATE name = VALUES(name);
