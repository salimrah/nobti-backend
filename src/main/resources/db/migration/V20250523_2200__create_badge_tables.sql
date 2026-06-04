CREATE TABLE IF NOT EXISTS provider_badge (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES provider(id) ON DELETE CASCADE,
    badge_type VARCHAR(30) NOT NULL,
    awarded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    is_auto_awarded BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(provider_id, badge_type)
);

CREATE INDEX IF NOT EXISTS idx_provider_badge_provider ON provider_badge(provider_id);
CREATE INDEX IF NOT EXISTS idx_provider_badge_type ON provider_badge(badge_type);
