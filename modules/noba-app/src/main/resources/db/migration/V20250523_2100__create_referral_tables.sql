CREATE TABLE IF NOT EXISTS referral (
    id UUID PRIMARY KEY,
    referrer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    referee_id UUID REFERENCES users(id) ON DELETE SET NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reward_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    reward_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_referral_referrer ON referral(referrer_id);
CREATE INDEX IF NOT EXISTS idx_referral_code ON referral(code);
CREATE INDEX IF NOT EXISTS idx_referral_status ON referral(status);
