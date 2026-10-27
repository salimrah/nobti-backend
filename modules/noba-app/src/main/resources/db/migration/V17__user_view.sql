-- V17: User view table — local read model of identity user data for decoupled user module
CREATE TABLE user_view (
    id UUID PRIMARY KEY,
    email TEXT,
    phone TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
