-- V19: Add index on provider location columns for nearby search queries
CREATE INDEX IF NOT EXISTS idx_provider_location
    ON provider (latitude, longitude);
