-- Expression GIN index backing the language-agnostic provider full-text search (name weighted 'A',
-- description 'B'). The search query evaluates this weighted expression directly, so no stored
-- tsvector column or trigger is required. Uses the 'simple' configuration, which performs no
-- language-specific stemming — appropriate for French / Moroccan-Arabic content.
CREATE INDEX IF NOT EXISTS idx_provider_search_simple
    ON provider USING GIN (
        (
            setweight(to_tsvector('simple', COALESCE(name, '')), 'A')
            || setweight(to_tsvector('simple', COALESCE(description, '')), 'B')
        )
    );
