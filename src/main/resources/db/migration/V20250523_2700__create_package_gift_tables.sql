-- ============================================================
-- V20250523_2700: Create service_package and gift_card tables
-- ============================================================

CREATE TABLE service_package (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id     UUID NOT NULL REFERENCES provider(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    price           DECIMAL(10,2) NOT NULL,
    original_price  DECIMAL(10,2) NOT NULL,
    valid_days      INT NOT NULL DEFAULT 30,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE package_service_item (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    package_id  UUID NOT NULL REFERENCES service_package(id) ON DELETE CASCADE,
    service_id  UUID NOT NULL REFERENCES provider_service(id),
    quantity    INT NOT NULL DEFAULT 1
);

CREATE TABLE gift_card (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id     UUID NOT NULL REFERENCES provider(id),
    buyer_id        UUID NOT NULL REFERENCES users(id),
    redeemer_id     UUID REFERENCES users(id),
    recipient_name  VARCHAR(255),
    amount          DECIMAL(10,2) NOT NULL,
    code            VARCHAR(20) NOT NULL,
    message         TEXT,
    redeemed        BOOLEAN NOT NULL DEFAULT false,
    redeemed_at     TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_gift_card_code ON gift_card(code);

CREATE INDEX idx_service_package_provider ON service_package(provider_id);
CREATE INDEX idx_package_service_item_package ON package_service_item(package_id);
CREATE INDEX idx_gift_card_buyer ON gift_card(buyer_id);
CREATE INDEX idx_gift_card_provider ON gift_card(provider_id);
