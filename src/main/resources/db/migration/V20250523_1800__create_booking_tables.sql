CREATE TABLE appointment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    provider_id UUID NOT NULL REFERENCES provider(id),
    service_id UUID REFERENCES provider_service(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_appointment_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_appointment_provider FOREIGN KEY (provider_id) REFERENCES provider(id),
    CONSTRAINT fk_appointment_service FOREIGN KEY (service_id) REFERENCES provider_service(id),
    CONSTRAINT chk_appointment_time CHECK (end_time > start_time)
);

CREATE INDEX idx_appointment_user ON appointment(user_id);
CREATE INDEX idx_appointment_provider ON appointment(provider_id);
CREATE INDEX idx_appointment_status ON appointment(status);
CREATE INDEX idx_appointment_start ON appointment(start_time);
CREATE INDEX idx_appointment_provider_date ON appointment(provider_id, start_time);

CREATE TABLE availability_slot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES provider(id),
    day_of_week SMALLINT CHECK (day_of_week BETWEEN 0 AND 6),
    specific_date DATE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_avail_provider FOREIGN KEY (provider_id) REFERENCES provider(id),
    CONSTRAINT chk_avail_time CHECK (end_time > start_time),
    CONSTRAINT chk_avail_day_or_date CHECK (
        (day_of_week IS NOT NULL AND specific_date IS NULL) OR
        (day_of_week IS NULL AND specific_date IS NOT NULL)
    )
);

CREATE INDEX idx_avail_provider ON availability_slot(provider_id);
CREATE INDEX idx_avail_day ON availability_slot(day_of_week);
CREATE INDEX idx_avail_date ON availability_slot(specific_date);
