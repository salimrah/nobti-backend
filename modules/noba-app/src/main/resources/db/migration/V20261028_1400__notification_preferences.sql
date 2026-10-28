-- Notification preferences v2: per category x channel matrix + per-user quiet hours/digest.

CREATE TABLE notification_channel_preference (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    category   VARCHAR(24) NOT NULL,
    channel    VARCHAR(16) NOT NULL,
    enabled    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_notification_channel_preference UNIQUE (user_id, category, channel)
);

CREATE INDEX idx_notification_channel_preference_user ON notification_channel_preference (user_id);

CREATE TABLE notification_setting (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL UNIQUE,
    quiet_hours_start TIME,
    quiet_hours_end   TIME,
    timezone          VARCHAR(64),
    digest            VARCHAR(16) NOT NULL DEFAULT 'NONE',
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);

-- Backfill the matrix from the legacy three booleans (all categories) for existing users.
INSERT INTO notification_channel_preference (id, user_id, category, channel, enabled, created_at, updated_at)
SELECT gen_random_uuid(), user_id, c.category, ch.channel,
       CASE ch.channel
           WHEN 'PUSH' THEN push_enabled
           WHEN 'EMAIL' THEN email_enabled
           ELSE sms_enabled
       END,
       now(), now()
FROM notification_preference
CROSS JOIN (VALUES ('TRANSACTIONAL'), ('MARKETING'), ('SYSTEM')) AS c(category)
CROSS JOIN (VALUES ('PUSH'), ('EMAIL'), ('SMS'), ('WHATSAPP')) AS ch(channel)
ON CONFLICT (user_id, category, channel) DO NOTHING;

INSERT INTO notification_setting (id, user_id, digest, created_at, updated_at)
SELECT gen_random_uuid(), user_id, 'NONE', now(), now()
FROM notification_preference
ON CONFLICT (user_id) DO NOTHING;
