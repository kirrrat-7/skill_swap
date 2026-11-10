-- Atomic, short-lived counters for abuse-sensitive actions. The key is a SHA-256 digest
-- of the client identifier; the raw address remains only in the normal security-event log.
CREATE TABLE rate_limit_buckets (
    bucket_key        CHAR(64)    NOT NULL,
    window_started_at DATETIME(6) NOT NULL,
    request_count     INT         NOT NULL DEFAULT 0,
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (bucket_key),
    KEY idx_rate_limit_updated (updated_at),
    CONSTRAINT chk_rate_limit_count CHECK (request_count >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
