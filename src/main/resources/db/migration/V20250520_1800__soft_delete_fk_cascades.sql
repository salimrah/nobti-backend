-- ============================================================
-- V20250520_1800: Soft-delete support & FK cascades
-- ============================================================
-- Adds ON DELETE CASCADE to tables that reference users(id)
-- but lack it, so the 30-day hard-purge job can clean up fully.
-- ============================================================

-- 1. notification_preference: has FK but no CASCADE → blocks user deletion
ALTER TABLE notification_preference
    DROP CONSTRAINT IF EXISTS fk_notification_preference_user,
    ADD CONSTRAINT fk_notification_preference_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 2. user_profile: no FK at all → add FK with CASCADE
-- Drop existing index, add FK, re-create index
DROP INDEX IF EXISTS idx_user_profile_user_id;
ALTER TABLE user_profile
    ADD CONSTRAINT fk_user_profile_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_user_profile_user_id ON user_profile(user_id);

-- 3. user_role: no FK at all → add FK with CASCADE
DROP INDEX IF EXISTS idx_user_role_user;
ALTER TABLE user_role
    ADD CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_user_role_user ON user_role(user_id);

-- 4. provider.user_id: no FK → add FK with SET NULL (keep historical data if provider is referenced)
ALTER TABLE provider
    DROP CONSTRAINT IF EXISTS fk_provider_user,
    ADD CONSTRAINT fk_provider_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- 5. provider_rating.customer_id: no FK → add FK with SET NULL
ALTER TABLE provider_rating
    DROP CONSTRAINT IF EXISTS fk_provider_rating_customer,
    ADD CONSTRAINT fk_provider_rating_customer
        FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE SET NULL;

-- 6. provider_favorite.customer_id: no FK → add FK with CASCADE (user preference)
ALTER TABLE provider_favorite
    DROP CONSTRAINT IF EXISTS fk_provider_favorite_customer,
    ADD CONSTRAINT fk_provider_favorite_customer
        FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE;

-- 7. queue_member.client_id: no FK → add FK with CASCADE
ALTER TABLE queue_member
    DROP CONSTRAINT IF EXISTS fk_queue_member_client,
    ADD CONSTRAINT fk_queue_member_client
        FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE;

-- 8. user_view: no FK → just clean up via job (no FK needed, it's a read model)
