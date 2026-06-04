CREATE TABLE IF NOT EXISTS provider_search_view (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    rating_average DOUBLE PRECISION DEFAULT 0.0,
    rating_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_provider_search_category ON provider_search_view(category);
CREATE INDEX idx_provider_search_active ON provider_search_view(is_active);
CREATE INDEX idx_provider_search_rating ON provider_search_view(rating_average DESC);
