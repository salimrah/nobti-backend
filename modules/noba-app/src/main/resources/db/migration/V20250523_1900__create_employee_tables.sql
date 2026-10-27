CREATE TABLE provider_employee (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES provider(id),
    user_id UUID REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    role VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_employee_provider FOREIGN KEY (provider_id) REFERENCES provider(id),
    CONSTRAINT fk_employee_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_employee_provider ON provider_employee(provider_id);
CREATE INDEX idx_employee_user ON provider_employee(user_id);

ALTER TABLE queue_member ADD COLUMN employee_id UUID REFERENCES provider_employee(id);
CREATE INDEX idx_queue_member_employee ON queue_member(employee_id);
