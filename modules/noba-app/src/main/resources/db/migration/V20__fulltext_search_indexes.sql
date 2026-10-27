-- V20: Add full-text search indexes for provider name and description

-- PostgreSQL tsvector GIN index for full-text search on provider name + description
ALTER TABLE provider ADD COLUMN IF NOT EXISTS search_vector tsvector;

CREATE INDEX IF NOT EXISTS idx_provider_search_vector
    ON provider USING GIN (search_vector);

-- Function to keep search_vector up to date
CREATE OR REPLACE FUNCTION provider_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector := to_tsvector('english', COALESCE(NEW.name, '') || ' ' || COALESCE(NEW.description, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update search_vector on insert or update
DROP TRIGGER IF EXISTS trg_provider_search_vector ON provider;
CREATE TRIGGER trg_provider_search_vector
    BEFORE INSERT OR UPDATE OF name, description
    ON provider
    FOR EACH ROW
    EXECUTE FUNCTION provider_search_vector_update();

-- Initialize search_vector for existing rows
UPDATE provider SET search_vector = to_tsvector('english', COALESCE(name, '') || ' ' || COALESCE(description, ''))
WHERE search_vector IS NULL;

-- Index for active+verified+category queries (composite covering index)
CREATE INDEX IF NOT EXISTS idx_provider_active_verified_category
    ON provider (active, verified, category)
    WHERE active = true AND verified = true;
