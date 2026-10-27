-- V21: Install PostgreSQL earthdistance extension for index-assisted geo queries
-- Requires: cube extension (earthdistance depends on it)

CREATE EXTENSION IF NOT EXISTS cube;
CREATE EXTENSION IF NOT EXISTS earthdistance;

-- GIST index for index-assisted bounding box filtering
-- earthdistance uses ll_to_earth(lat, lon) to convert lat/lon to earth coordinates
CREATE INDEX IF NOT EXISTS idx_provider_earth_location
    ON provider USING gist (ll_to_earth(latitude, longitude));
