CREATE TABLE IF NOT EXISTS webhook_delivery_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    status_code INTEGER NOT NULL DEFAULT 0,
    response_body TEXT,
    success BOOLEAN NOT NULL DEFAULT false,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_delivery_log_subscription ON webhook_delivery_log(subscription_id);
CREATE INDEX IF NOT EXISTS idx_delivery_log_created ON webhook_delivery_log(created_at);
