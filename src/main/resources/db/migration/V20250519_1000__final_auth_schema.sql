-- ============================================================
-- V20250519_1000: Final Auth Schema Migration
-- ============================================================
-- Creates new tables, migrates data from old tables, drops old tables.
-- After this migration: 11 auth tables + Redis for OTP/rate limiting.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- 1. ALTER users → final columns
-- ============================================================
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_failed_login_at TIMESTAMPTZ;

-- Rename email → primary_email, phone → primary_phone
ALTER TABLE users RENAME COLUMN email TO primary_email;
ALTER TABLE users RENAME COLUMN email_hash TO primary_email_hash;
ALTER TABLE users RENAME COLUMN phone TO primary_phone;
ALTER TABLE users RENAME COLUMN phone_hash TO primary_phone_hash;

-- Rename verified columns
ALTER TABLE users RENAME COLUMN email_verified TO primary_email_verified;
ALTER TABLE users RENAME COLUMN phone_verified TO primary_phone_verified;

-- Recreate indexes with new names
DROP INDEX IF EXISTS idx_users_email_hash;
DROP INDEX IF EXISTS idx_users_phone_hash;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_primary_email_hash
ON users(primary_email_hash) WHERE primary_email_hash IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_primary_phone_hash
ON users(primary_phone_hash) WHERE primary_phone_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);


-- ============================================================
-- 2. CREATE user_identities (replaces social_accounts)
-- ============================================================
CREATE TABLE user_identities (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_type               VARCHAR(30) NOT NULL
                                CHECK (provider_type IN ('email','phone','google','apple','github','facebook')),
    identifier_encrypted        TEXT,
    identifier_hash             VARCHAR(64) NOT NULL,
    normalized_identifier       VARCHAR(255) NOT NULL,
    is_primary                  BOOLEAN NOT NULL DEFAULT FALSE,
    status                      VARCHAR(20) NOT NULL DEFAULT 'active',
    verified_at                 TIMESTAMPTZ,
    reverified_at               TIMESTAMPTZ,
    scopes                      TEXT,
    refresh_token_expires_at    TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_provider_identifier_hash UNIQUE (provider_type, identifier_hash)
);

CREATE INDEX idx_user_identities_user ON user_identities (user_id);
CREATE INDEX idx_user_identities_lookup ON user_identities (provider_type, identifier_hash);
CREATE INDEX idx_user_identities_primary ON user_identities (user_id, is_primary);

-- Migrate data from social_accounts
INSERT INTO user_identities (id, user_id, provider_type, identifier_hash, normalized_identifier, is_primary, status, verified_at, created_at, updated_at)
SELECT
    sa.id,
    sa.user_id,
    LOWER(sa.provider),
    ENCODE(DIGEST(LOWER(sa.provider) || ':' || sa.subject, 'sha256'), 'hex'),
    LOWER(sa.provider) || ':' || sa.subject,
    TRUE,
    'active',
    sa.linked_at,
    sa.linked_at,
    sa.linked_at
FROM social_accounts sa;


-- ============================================================
-- 3. CREATE credential_passwords (replaces credentials WHERE type='PASSWORD')
-- ============================================================
CREATE TABLE credential_passwords (
    user_id                 UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    password_hash           VARCHAR(255) NOT NULL,
    password_updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    must_change_password    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO credential_passwords (user_id, password_hash, created_at, updated_at)
SELECT user_id, secret_hash, created_at, created_at
FROM credentials
WHERE type = 'PASSWORD';


-- ============================================================
-- 4. CREATE credential_passkeys (future — empty stub)
-- ============================================================
CREATE TABLE credential_passkeys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    credential_id   BYTEA NOT NULL,
    public_key      BYTEA NOT NULL,
    counter         BIGINT NOT NULL DEFAULT 0,
    aaguid          UUID,
    device_name     VARCHAR(100),
    transports      TEXT[],
    backed_up       BOOLEAN,
    device_type     VARCHAR(30),
    sign_count      BIGINT NOT NULL DEFAULT 0,
    last_ip         INET,
    last_user_agent TEXT,
    last_used_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_passkey_credential_id UNIQUE (credential_id)
);

CREATE INDEX idx_passkeys_user ON credential_passkeys (user_id);


-- ============================================================
-- 5. CREATE credential_totp (future — empty stub)
-- ============================================================
CREATE TABLE credential_totp (
    user_id         UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    secret_encrypted TEXT NOT NULL,
    is_enabled      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 6. CREATE credential_recovery_codes (future — empty stub)
-- ============================================================
CREATE TABLE credential_recovery_codes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash   VARCHAR(255) NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recovery_codes_user ON credential_recovery_codes (user_id);


-- ============================================================
-- 7. CREATE devices (physical trusted devices)
-- ============================================================
CREATE TABLE devices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_name     VARCHAR(255),
    platform        VARCHAR(50),
    app_version     VARCHAR(50),
    fcm_token       VARCHAR(255),
    last_ip_address VARCHAR(45),
    last_user_agent TEXT,
    trusted         BOOLEAN NOT NULL DEFAULT FALSE,
    trust_score     INT NOT NULL DEFAULT 0,
    last_seen_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_devices_user ON devices (user_id);


-- ============================================================
-- 8. ALTER refresh_tokens → final columns
-- ============================================================
ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS family_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS mfa_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS issued_ip VARCHAR(45),
    ADD COLUMN IF NOT EXISTS issued_user_agent TEXT,
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS revoke_reason VARCHAR(100),
    ADD COLUMN IF NOT EXISTS reused_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS device_fingerprint_hash VARCHAR(64),
    DROP COLUMN IF EXISTS client_id,
    ALTER COLUMN token_hash SET NOT NULL,
    ALTER COLUMN expires_at SET NOT NULL;

-- Set revoked_at for existing revoked rows
UPDATE refresh_tokens SET revoked_at = last_used_at WHERE revoked = TRUE;
ALTER TABLE refresh_tokens DROP COLUMN IF EXISTS revoked;

-- Drop old indexes, create new ones
DROP INDEX IF EXISTS idx_refresh_token_hash;
DROP INDEX IF EXISTS idx_refresh_user;

CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_device ON refresh_tokens (device_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active ON refresh_tokens (user_id, expires_at) WHERE revoked_at IS NULL;


-- ============================================================
-- 9. CREATE verification_challenges (PG for password_reset + email_verify only)
-- OTP challenges stay in Redis.
-- Replaces: password_reset_tokens + email_verification_codes
-- ============================================================
CREATE TABLE verification_challenges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    challenge_type  VARCHAR(30) NOT NULL,
    purpose         VARCHAR(30),
    code_hash       VARCHAR(255),
    attempts        INT NOT NULL DEFAULT 0,
    max_attempts    INT NOT NULL DEFAULT 5,
    next_resend_at  TIMESTAMPTZ,
    delivery_status VARCHAR(20),
    expires_at      TIMESTAMPTZ NOT NULL,
    consumed_at     TIMESTAMPTZ,
    created_ip      VARCHAR(45),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_verification_user ON verification_challenges (user_id);
CREATE INDEX idx_verification_active ON verification_challenges (expires_at) WHERE consumed_at IS NULL;

-- Migrate password_reset_tokens
INSERT INTO verification_challenges (id, user_id, challenge_type, code_hash, attempts, max_attempts, expires_at, consumed_at, created_at)
SELECT
    id,
    user_id,
    'password_reset',
    token,
    0, 5, expires_at,
    CASE WHEN used = TRUE THEN created_at ELSE NULL END,
    created_at
FROM password_reset_tokens;

-- Migrate email_verification_codes
INSERT INTO verification_challenges (id, user_id, challenge_type, code_hash, attempts, max_attempts, expires_at, consumed_at, created_at)
SELECT
    evc.id,
    u.id,
    'email_verify',
    evc.code,
    0, 5, evc.expires_at,
    CASE WHEN evc.status = 'VERIFIED' THEN evc.created_at ELSE NULL END,
    evc.created_at
FROM email_verification_codes evc
JOIN users u ON u.primary_email_hash = ENCODE(DIGEST(LOWER(evc.email), 'sha256'), 'hex');


-- ============================================================
-- 10. CREATE security_logs (replaces audit_logs)
-- ============================================================
CREATE TABLE security_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    event_type  VARCHAR(100) NOT NULL,
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    payload     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_security_logs_user_time ON security_logs (user_id, created_at DESC);
CREATE INDEX idx_security_logs_type_time ON security_logs (event_type, created_at DESC);

-- Migrate audit_logs
INSERT INTO security_logs (user_id, event_type, ip_address, user_agent, payload, created_at)
SELECT
    CASE WHEN user_id ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
         THEN user_id::UUID ELSE NULL END,
    event_type,
    NULLIF(ip, ''),
    user_agent,
    NULLIF(NULLIF(metadata_json, ''), '{}'),
    created_at
FROM audit_logs;


-- ============================================================
-- 11. Migrate old device table → devices, then drop
-- ============================================================
INSERT INTO devices (id, user_id, platform, fcm_token, created_at, last_seen_at)
SELECT
    id, user_id, platform, fcm_token, registered_at, registered_at
FROM device
ON CONFLICT (id) DO NOTHING;

DROP TABLE IF EXISTS device CASCADE;

-- ============================================================
-- 12. DROP old auth tables
-- ============================================================
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS password_reset_tokens CASCADE;
DROP TABLE IF EXISTS email_verification_codes CASCADE;
DROP TABLE IF EXISTS credentials CASCADE;
DROP TABLE IF EXISTS social_accounts CASCADE;

-- ============================================================
-- 13. CREATE account_link_events (linking history / audit)
-- ============================================================
CREATE TABLE account_link_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_user_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    provider_type   VARCHAR(30) NOT NULL,
    action          VARCHAR(20) NOT NULL CHECK (action IN ('link','unlink','merge','split')),
    reason          VARCHAR(100),
    payload         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_account_link_events_user ON account_link_events (user_id);
CREATE INDEX idx_account_link_events_actor ON account_link_events (actor_user_id);
CREATE INDEX idx_account_link_events_time ON account_link_events (created_at DESC);

-- ============================================================
-- 14. CREATE user_consents (GDPR / privacy tracking)
-- ============================================================
CREATE TABLE user_consents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_type        VARCHAR(50) NOT NULL
                        CHECK (consent_type IN ('privacy_policy','terms_of_service','marketing','data_processing','cookie_consent')),
    granted             BOOLEAN NOT NULL,
    consent_version     VARCHAR(20),
    ip_address          VARCHAR(45),
    user_agent          TEXT,
    granted_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at          TIMESTAMPTZ,

    CONSTRAINT uq_user_consent_type UNIQUE (user_id, consent_type)
);

CREATE INDEX idx_user_consents_user ON user_consents (user_id);

-- ============================================================
-- 15. Additional partial indexes for query performance
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_users_locked ON users (locked_until) WHERE locked_until IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_identities_status ON user_identities (status) WHERE status != 'active';
CREATE INDEX IF NOT EXISTS idx_credentials_must_change ON credential_passwords (must_change_password) WHERE must_change_password = TRUE;
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_reuse ON refresh_tokens (reused_at) WHERE reused_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_devices_trusted ON devices (trust_score) WHERE trust_score > 0;
