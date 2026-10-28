-- Platform broadcast campaigns sent from the admin console.

CREATE TABLE notification_campaign (
    id               UUID PRIMARY KEY,
    title            VARCHAR(160) NOT NULL,
    body             TEXT         NOT NULL,
    audience         VARCHAR(24)  NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    total_recipients INTEGER      NOT NULL DEFAULT 0,
    sent_count       INTEGER      NOT NULL DEFAULT 0,
    failed_count     INTEGER      NOT NULL DEFAULT 0,
    created_by       UUID         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_notification_campaign_created ON notification_campaign (created_at DESC);
