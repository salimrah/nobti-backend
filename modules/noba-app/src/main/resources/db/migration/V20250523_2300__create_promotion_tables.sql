CREATE TABLE IF NOT EXISTS promotion (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES provider(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    discount_percent NUMERIC(5,2) NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    max_redemptions INTEGER,
    current_redemptions INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS promotion_service (
    id UUID PRIMARY KEY,
    promotion_id UUID NOT NULL REFERENCES promotion(id) ON DELETE CASCADE,
    service_id UUID NOT NULL,
    UNIQUE(promotion_id, service_id)
);

CREATE INDEX IF NOT EXISTS idx_promotion_provider ON promotion(provider_id);
CREATE INDEX IF NOT EXISTS idx_promotion_active ON promotion(is_active);
CREATE INDEX IF NOT EXISTS idx_promotion_dates ON promotion(valid_from, valid_to);
