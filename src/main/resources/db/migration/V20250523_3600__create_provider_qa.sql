CREATE TABLE provider_qa (
    id          UUID PRIMARY KEY,
    provider_id UUID NOT NULL REFERENCES provider(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question    TEXT NOT NULL,
    answer      TEXT,
    answered_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_provider_qa_provider_id ON provider_qa(provider_id);
CREATE INDEX idx_provider_qa_user_id ON provider_qa(user_id);
