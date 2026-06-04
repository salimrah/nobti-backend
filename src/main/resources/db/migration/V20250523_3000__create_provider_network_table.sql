-- ============================================================
-- V20250523_3000: Create provider_network table
-- ============================================================

CREATE TABLE provider_network (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id         UUID NOT NULL REFERENCES provider(id),
    partner_id          UUID NOT NULL REFERENCES provider(id),
    partner_name        VARCHAR(200),
    relationship_type   VARCHAR(50),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_provider_network_pair UNIQUE (provider_id, partner_id)
);

CREATE INDEX idx_provider_network_provider ON provider_network(provider_id);
