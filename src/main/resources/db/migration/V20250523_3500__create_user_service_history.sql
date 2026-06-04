CREATE TABLE user_service_history (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    provider_name VARCHAR(200),
    service_id VARCHAR(255),
    service_name VARCHAR(200),
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    price NUMERIC(10, 2),
    rating INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ush_user ON user_service_history(user_id);
CREATE INDEX idx_ush_user_date ON user_service_history(user_id, completed_at DESC);
