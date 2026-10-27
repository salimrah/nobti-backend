CREATE TABLE service_reminder (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    service_id UUID,
    provider_name VARCHAR(200) NOT NULL,
    service_name VARCHAR(200) NOT NULL,
    interval_days INTEGER NOT NULL,
    next_reminder_at TIMESTAMP WITH TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_reminder_next ON service_reminder(next_reminder_at) WHERE active = true;
CREATE INDEX idx_reminder_user ON service_reminder(user_id);
