CREATE TABLE notification_preference (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_notification_preference_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_notification_preference_user_id ON notification_preference(user_id);
