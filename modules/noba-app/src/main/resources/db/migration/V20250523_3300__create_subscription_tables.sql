CREATE TABLE provider_subscription (
    id UUID PRIMARY KEY,
    provider_id UUID NOT NULL UNIQUE,
    tier VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    renews_at TIMESTAMP WITH TIME ZONE,
    stripe_subscription_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_subscription_provider ON provider_subscription(provider_id);
CREATE INDEX idx_subscription_tier ON provider_subscription(tier);
CREATE INDEX idx_subscription_status ON provider_subscription(status);
