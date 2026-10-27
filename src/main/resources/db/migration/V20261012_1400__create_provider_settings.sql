-- Per-provider configuration (queue rules, policies, loyalty, locale).
-- Absent rows fall back to sensible defaults in the application layer, so no backfill is needed.
CREATE TABLE IF NOT EXISTS provider_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL,
    max_party_size INTEGER NOT NULL DEFAULT 10,
    walk_ins_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    appointments_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    no_show_grace_minutes INTEGER NOT NULL DEFAULT 10,
    cancellation_window_minutes INTEGER NOT NULL DEFAULT 60,
    auto_fill_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    loyalty_points_per_visit INTEGER NOT NULL DEFAULT 10,
    notification_locale VARCHAR(8) NOT NULL DEFAULT 'en',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_provider_settings_provider UNIQUE (provider_id)
);
