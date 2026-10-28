-- Notification subsystem: intent, per-channel delivery attempts, append-only audit events.

CREATE TABLE notification (
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL,
    type             VARCHAR(80)  NOT NULL,
    category         VARCHAR(24)  NOT NULL,
    priority         INTEGER      NOT NULL DEFAULT 1,
    title            VARCHAR(160) NOT NULL,
    body             TEXT         NOT NULL,
    data             JSONB,
    template_key     VARCHAR(80),
    template_version INTEGER,
    locale           VARCHAR(16),
    dedupe_key       VARCHAR(160),
    status           VARCHAR(16)  NOT NULL,
    scheduled_at     TIMESTAMPTZ,
    expires_at       TIMESTAMPTZ,
    read_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX uq_notification_dedupe ON notification (dedupe_key) WHERE dedupe_key IS NOT NULL;
CREATE INDEX idx_notification_user_created ON notification (user_id, created_at DESC);
CREATE INDEX idx_notification_status_scheduled ON notification (status, scheduled_at);

CREATE TABLE notification_delivery (
    id                  UUID PRIMARY KEY,
    notification_id     UUID        NOT NULL REFERENCES notification (id) ON DELETE CASCADE,
    channel             VARCHAR(16) NOT NULL,
    provider            VARCHAR(32),
    recipient_masked    VARCHAR(120),
    recipient_hash      VARCHAR(64),
    status              VARCHAR(16) NOT NULL,
    attempt             INTEGER     NOT NULL DEFAULT 0,
    provider_message_id VARCHAR(160),
    error_code          VARCHAR(64),
    error_message       TEXT,
    latency_ms          INTEGER,
    queued_at           TIMESTAMPTZ,
    sent_at             TIMESTAMPTZ,
    delivered_at        TIMESTAMPTZ,
    opened_at           TIMESTAMPTZ,
    clicked_at          TIMESTAMPTZ,
    next_attempt_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_delivery_notification ON notification_delivery (notification_id);
CREATE INDEX idx_delivery_due ON notification_delivery (status, next_attempt_at);
CREATE INDEX idx_delivery_provider_message ON notification_delivery (provider_message_id);

CREATE TABLE notification_event (
    id              BIGSERIAL PRIMARY KEY,
    notification_id UUID        NOT NULL,
    delivery_id     UUID,
    event_type      VARCHAR(32) NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    metadata        JSONB
);

CREATE INDEX idx_notification_event_notification ON notification_event (notification_id, occurred_at);
CREATE INDEX idx_notification_event_type_time ON notification_event (event_type, occurred_at);

CREATE TABLE notification_template (
    id             UUID PRIMARY KEY,
    template_key   VARCHAR(80)  NOT NULL,
    channel        VARCHAR(16)  NOT NULL,
    locale         VARCHAR(16)  NOT NULL,
    version        INTEGER      NOT NULL,
    subject        VARCHAR(200),
    body           TEXT         NOT NULL,
    variables      JSONB,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_notification_template UNIQUE (template_key, channel, locale, version)
);
