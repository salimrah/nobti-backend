-- V13_1: Create user module tables
-- Replaces the v1 tables dropped by V13 with user-module equivalents.

CREATE TABLE IF NOT EXISTS device (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    fcm_token VARCHAR(255) NOT NULL,
    platform VARCHAR(20),
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_user_fcm UNIQUE (user_id, fcm_token)
);

CREATE INDEX IF NOT EXISTS idx_device_user ON device(user_id);
CREATE INDEX IF NOT EXISTS idx_device_fcm_token ON device(fcm_token) WHERE fcm_token IS NOT NULL;

CREATE TABLE IF NOT EXISTS user_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    profile_picture_url TEXT,
    preferred_language VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_profile_user_id ON user_profile(user_id);

CREATE TABLE IF NOT EXISTS user_role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    role_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    activated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_role_user ON user_role(user_id);
