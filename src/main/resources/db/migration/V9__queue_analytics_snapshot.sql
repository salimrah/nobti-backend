CREATE TABLE IF NOT EXISTS queue_analytics_snapshot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL,
    date DATE NOT NULL,
    total_served INTEGER NOT NULL DEFAULT 0,
    avg_wait_time_minutes DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    avg_service_duration_minutes DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    peak_hour INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_snapshot_provider_date UNIQUE (provider_id, date)
);

CREATE INDEX IF NOT EXISTS idx_snapshot_provider ON queue_analytics_snapshot(provider_id);
