-- ============================================================
-- V20250523_2900: Create announcement table
-- ============================================================

CREATE TABLE announcement (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id     UUID NOT NULL REFERENCES provider(id),
    title           VARCHAR(255) NOT NULL,
    message         TEXT,
    important       BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP
);

CREATE INDEX idx_announcement_provider ON announcement(provider_id);
CREATE INDEX idx_announcement_active ON announcement(provider_id, expires_at);
