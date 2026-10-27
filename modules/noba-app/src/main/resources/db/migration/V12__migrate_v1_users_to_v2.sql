-- V12: Migrate v1 users to v2 identity tables
-- Runs after V8 creates the v2 tables.
-- Only inserts rows that don't already exist (safe for dual-write overlap).
-- Encrypted fields (email, phone) are set to NULL — the application
-- re-encrypts them on next user profile update.
-- Requires PostgreSQL 14+ (uses built-in sha256)

-- 1. Migrate app_user + user_profile → users
INSERT INTO users (id, email, email_hash, phone, phone_hash, display_name, status, created_at, updated_at)
SELECT
    au.id,
    NULL,
    CASE WHEN au.email IS NOT NULL THEN encode(sha256(lower(au.email)::bytea), 'hex') END,
    NULL,
    CASE WHEN au.phone IS NOT NULL THEN encode(sha256(au.phone::bytea), 'hex') END,
    TRIM(COALESCE(up.first_name, '') || ' ' || COALESCE(up.last_name, '')),
    CAST(au.status AS VARCHAR(20)),
    au.created_at,
    au.updated_at
FROM app_user au
LEFT JOIN user_profile up ON up.app_user_id = au.id
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = au.id);

-- 2. Migrate auth_credential → credentials
INSERT INTO credentials (id, user_id, type, secret_hash, created_at)
SELECT
    ac.id,
    ac.user_id,
    CASE ac.identity_type
        WHEN 'EMAIL' THEN 'PASSWORD'
        WHEN 'PHONE' THEN 'OTP'
        ELSE 'PASSWORD'
    END,
    ac.password_hash,
    ac.created_at
FROM auth_credential ac
WHERE NOT EXISTS (
    SELECT 1 FROM credentials c
    WHERE c.user_id = ac.user_id
      AND c.type = CASE ac.identity_type
                        WHEN 'EMAIL' THEN 'PASSWORD'
                        WHEN 'PHONE' THEN 'OTP'
                        ELSE 'PASSWORD'
                   END
);

-- 3. Migrate email_verification → email_verification_codes
INSERT INTO email_verification_codes (id, email, code, status, expires_at, created_at)
SELECT
    ev.id,
    ev.email,
    ev.code,
    ev.status,
    ev.expires_at,
    ev.created_at
FROM email_verification ev
WHERE NOT EXISTS (SELECT 1 FROM email_verification_codes evc WHERE evc.email = ev.email AND evc.code = ev.code);

-- 4. Migrate password_reset_token → password_reset_tokens
INSERT INTO password_reset_tokens (id, user_id, token, expires_at, used, created_at)
SELECT
    prt.id,
    prt.user_id,
    prt.token,
    prt.expires_at,
    prt.used,
    prt.created_at
FROM password_reset_token prt
WHERE NOT EXISTS (SELECT 1 FROM password_reset_tokens prt2 WHERE prt2.token = prt.token);

-- 5. Migrate linked_accounts → social_accounts
INSERT INTO social_accounts (id, user_id, provider, subject, email, raw_profile_json, linked_at)
SELECT
    la.id,
    la.user_id,
    la.provider,
    la.provider_subject_id,
    la.email,
    NULL,
    la.created_at
FROM linked_accounts la
WHERE NOT EXISTS (
    SELECT 1 FROM social_accounts sa
    WHERE sa.provider = la.provider AND sa.subject = la.provider_subject_id
);
