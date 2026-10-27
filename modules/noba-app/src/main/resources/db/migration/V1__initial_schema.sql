-- V1: Initial schema baseline
-- Covers all entities as of May 2026

-- Core user tables
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    phone VARCHAR(50),
    email VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_user_id UUID UNIQUE NOT NULL REFERENCES app_user(id),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    profile_picture_url VARCHAR(255),
    preferred_language VARCHAR(10),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id),
    role_type VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    activated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS device (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id),
    fcm_token VARCHAR(500) NOT NULL,
    platform VARCHAR(50),
    registered_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS business_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES app_user(id),
    business_name VARCHAR(255),
    business_logo VARCHAR(255),
    address VARCHAR(500),
    description TEXT,
    verified BOOLEAN NOT NULL DEFAULT false
);

-- Auth tables
CREATE TABLE IF NOT EXISTS auth_credential (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id),
    identity_type VARCHAR(20) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    password_hash TEXT,
    verified BOOLEAN NOT NULL DEFAULT false,
    primary_credential BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_auth_credential_identifier_type UNIQUE (identifier, identity_type)
);

CREATE TABLE IF NOT EXISTS otp_verification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number VARCHAR(50) NOT NULL,
    code VARCHAR(10) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_verification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS password_reset_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL
);

-- Provider tables
CREATE TABLE IF NOT EXISTS provider (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    logo_url VARCHAR(255),
    address VARCHAR(500),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    category VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT true,
    verified BOOLEAN NOT NULL DEFAULT false,
    verification_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS provider_service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES provider(id),
    name VARCHAR(255) NOT NULL,
    duration INTERVAL,
    price DECIMAL(10,2),
    active BOOLEAN NOT NULL DEFAULT true,
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS provider_rating (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    value INTEGER NOT NULL CHECK (value >= 1 AND value <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_provider_rating UNIQUE (provider_id, customer_id)
);

CREATE TABLE IF NOT EXISTS provider_favorite (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_provider_favorite UNIQUE (provider_id, customer_id)
);

CREATE TABLE IF NOT EXISTS service_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    default_duration INTERVAL,
    description TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS business_hours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL,
    day_of_week INTEGER NOT NULL,
    opens_at TIME,
    closes_at TIME,
    closed BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_business_hours_provider_day UNIQUE (provider_id, day_of_week)
);

-- Queue tables
CREATE TABLE IF NOT EXISTS queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS queue_member (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    queue_id UUID NOT NULL REFERENCES queue(id),
    client_id UUID NOT NULL,
    service_id VARCHAR(255),
    position INTEGER NOT NULL,
    estimated_duration INTERVAL,
    estimated_start_time TIMESTAMP,
    actual_start_time TIMESTAMP,
    completed_time TIMESTAMP,
    actual_duration INTERVAL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    version BIGINT NOT NULL DEFAULT 0,
    notified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_queue_member_position UNIQUE (queue_id, position)
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_app_user_phone ON app_user(phone);
CREATE INDEX IF NOT EXISTS idx_app_user_email ON app_user(email);
CREATE INDEX IF NOT EXISTS idx_user_role_user ON user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_device_user ON device(user_id);
CREATE INDEX IF NOT EXISTS idx_device_fcm ON device(fcm_token);
CREATE INDEX IF NOT EXISTS idx_auth_credential_identifier ON auth_credential(identifier);
CREATE INDEX IF NOT EXISTS idx_otp_phone ON otp_verification(phone_number);
CREATE INDEX IF NOT EXISTS idx_otp_status ON otp_verification(status);
CREATE INDEX IF NOT EXISTS idx_provider_active ON provider(active);
CREATE INDEX IF NOT EXISTS idx_provider_category ON provider(category);
CREATE INDEX IF NOT EXISTS idx_provider_service_provider ON provider_service(provider_id);
CREATE INDEX IF NOT EXISTS idx_provider_rating_provider ON provider_rating(provider_id);
CREATE INDEX IF NOT EXISTS idx_queue_provider ON queue(provider_id);
CREATE INDEX IF NOT EXISTS idx_queue_member_queue ON queue_member(queue_id);
CREATE INDEX IF NOT EXISTS idx_queue_member_client ON queue_member(client_id);
CREATE INDEX IF NOT EXISTS idx_queue_member_status ON queue_member(status);
CREATE INDEX IF NOT EXISTS idx_business_hours_provider ON business_hours(provider_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_token ON password_reset_token(token);
CREATE INDEX IF NOT EXISTS idx_password_reset_user ON password_reset_token(user_id);

