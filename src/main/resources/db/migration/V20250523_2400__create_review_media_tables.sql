CREATE TABLE IF NOT EXISTS review_media (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES provider_rating(id) ON DELETE CASCADE,
    url VARCHAR(2048) NOT NULL,
    media_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
    is_moderated BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_review_media_review ON review_media(review_id);
