-- Notification templates (localized copy with {{placeholders}}) and digest batching.

-- channel 'ANY' means the template is channel-agnostic (used for the message title/body).
INSERT INTO notification_template (id, template_key, channel, locale, version, subject, body, variables, active, created_at) VALUES
    (gen_random_uuid(), 'queue.turn_start', 'ANY', 'en', 1,
     'It''s your turn!',
     'It''s your turn at the service provider. Please arrive within 5 minutes.',
     '[]', TRUE, now()),
    (gen_random_uuid(), 'queue.turn_start', 'ANY', 'fr', 1,
     'C''est votre tour !',
     'C''est votre tour chez le prestataire. Merci d''arriver dans les 5 minutes.',
     '[]', TRUE, now()),
    (gen_random_uuid(), 'queue.estimated_wait', 'ANY', 'en', 1,
     'Queue update',
     'Estimated wait: {{minutes}} min.',
     '["minutes"]', TRUE, now()),
    (gen_random_uuid(), 'queue.estimated_wait', 'ANY', 'fr', 1,
     'File d''attente',
     'Attente estimée : {{minutes}} min.',
     '["minutes"]', TRUE, now()),
    (gen_random_uuid(), 'queue.waitlist_open', 'ANY', 'en', 1,
     'A spot is open',
     'The queue you were waiting for is now open. Join now!',
     '[]', TRUE, now()),
    (gen_random_uuid(), 'queue.waitlist_open', 'ANY', 'fr', 1,
     'Une place s''est libérée',
     'La file d''attente que vous suiviez est maintenant ouverte. Rejoignez-la !',
     '[]', TRUE, now()),
    (gen_random_uuid(), 'reminder.service', 'ANY', 'en', 1,
     'Service Reminder',
     'It''s time for your {{serviceName}} at {{providerName}}!',
     '["serviceName","providerName"]', TRUE, now()),
    (gen_random_uuid(), 'reminder.service', 'ANY', 'fr', 1,
     'Rappel de service',
     'C''est l''heure de votre {{serviceName}} chez {{providerName}} !',
     '["serviceName","providerName"]', TRUE, now()),
    (gen_random_uuid(), 'digest.summary', 'ANY', 'en', 1,
     'Your updates',
     'You have {{count}} new updates.',
     '["count"]', TRUE, now()),
    (gen_random_uuid(), 'digest.summary', 'ANY', 'fr', 1,
     'Vos actualités',
     'Vous avez {{count}} nouvelles actualités.',
     '["count"]', TRUE, now());

CREATE TABLE notification_digest_entry (
    id              UUID PRIMARY KEY,
    user_id         UUID        NOT NULL,
    notification_id UUID        NOT NULL REFERENCES notification (id) ON DELETE CASCADE,
    digest_id       UUID,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_notification_digest_entry UNIQUE (notification_id)
);

CREATE INDEX idx_notification_digest_pending ON notification_digest_entry (user_id) WHERE digest_id IS NULL;
CREATE INDEX idx_notification_digest_id ON notification_digest_entry (digest_id);
