CREATE TABLE IF NOT EXISTS loyalty_points (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES provider(id) ON DELETE CASCADE,
    total_points INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, provider_id)
);

CREATE TABLE IF NOT EXISTS loyalty_points_transaction (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_id UUID NOT NULL REFERENCES provider(id) ON DELETE CASCADE,
    points INTEGER NOT NULL,
    reason VARCHAR(100) NOT NULL,
    reference_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loyalty_reward (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES provider(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    points_required INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loyalty_redemption (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reward_id UUID NOT NULL REFERENCES loyalty_reward(id) ON DELETE CASCADE,
    points_spent INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loyalty_points_user_provider ON loyalty_points(user_id, provider_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_points_provider ON loyalty_points(provider_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_txn_user ON loyalty_points_transaction(user_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_txn_provider ON loyalty_points_transaction(provider_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_reward_provider ON loyalty_reward(provider_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_redemption_user ON loyalty_redemption(user_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_redemption_reward ON loyalty_redemption(reward_id);
