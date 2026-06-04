CREATE TABLE provider_location (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES provider(id),
    name VARCHAR(200) NOT NULL,
    address VARCHAR(300),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    phone VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_location_provider FOREIGN KEY (provider_id) REFERENCES provider(id)
);

CREATE INDEX idx_location_provider ON provider_location(provider_id);
CREATE INDEX idx_location_provider_active ON provider_location(provider_id, is_active);

ALTER TABLE provider_employee ADD COLUMN location_id UUID REFERENCES provider_location(id);
CREATE INDEX idx_employee_location ON provider_employee(location_id);
