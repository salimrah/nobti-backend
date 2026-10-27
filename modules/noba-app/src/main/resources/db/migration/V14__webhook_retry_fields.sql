-- V14: Add retry tracking fields to webhook_delivery_log

ALTER TABLE webhook_delivery_log
    ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_delivery_log_retry ON webhook_delivery_log(next_retry_at)
    WHERE next_retry_at IS NOT NULL AND success = false;
