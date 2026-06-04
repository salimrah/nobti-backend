-- ============================================================
-- V20250523_2800: Create provider_media table for portfolio gallery
-- ============================================================

CREATE TABLE provider_media (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id     UUID NOT NULL REFERENCES provider(id),
    url             VARCHAR(2048) NOT NULL,
    caption         VARCHAR(500),
    category        VARCHAR(50),
    media_order     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_provider_media_provider ON provider_media(provider_id);
CREATE INDEX idx_provider_media_order ON provider_media(provider_id, media_order);
