-- V18: Add version column for optimistic locking on user_view
ALTER TABLE user_view ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
