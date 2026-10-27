CREATE TABLE payment_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    provider_id UUID NOT NULL REFERENCES provider(id),
    service_id UUID REFERENCES provider_service(id),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    amount DECIMAL(10, 2) NOT NULL,
    fee DECIMAL(10, 2),
    net_amount DECIMAL(10, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    stripe_payment_intent_id VARCHAR(100),
    stripe_charge_id VARCHAR(100),
    failure_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_payment_provider FOREIGN KEY (provider_id) REFERENCES provider(id),
    CONSTRAINT fk_payment_service FOREIGN KEY (service_id) REFERENCES provider_service(id)
);

CREATE INDEX idx_payment_user_id ON payment_transaction(user_id);
CREATE INDEX idx_payment_provider_id ON payment_transaction(provider_id);
CREATE INDEX idx_payment_status ON payment_transaction(status);
CREATE INDEX idx_payment_stripe_pi ON payment_transaction(stripe_payment_intent_id);

CREATE TABLE payout (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES provider(id),
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    stripe_transfer_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMP,
    CONSTRAINT fk_payout_provider FOREIGN KEY (provider_id) REFERENCES provider(id)
);

CREATE INDEX idx_payout_provider_id ON payout(provider_id);
CREATE INDEX idx_payout_status ON payout(status);

CREATE TABLE provider_payout_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL UNIQUE REFERENCES provider(id),
    stripe_account_id VARCHAR(100),
    payouts_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payout_settings_provider FOREIGN KEY (provider_id) REFERENCES provider(id)
);

CREATE INDEX idx_payout_settings_provider ON provider_payout_settings(provider_id);
