-- V20250516_1200: Move display_name from users table to user_profile table
-- display_name is profile data, not auth data. Every user gets a UserProfile.

ALTER TABLE user_profile ADD COLUMN display_name VARCHAR(255);

-- Migrate existing display_name values from users to user_profile
UPDATE user_profile up
SET display_name = u.display_name
FROM users u
WHERE up.user_id = u.id AND u.display_name IS NOT NULL;

-- Create user_profiles for users who have display_name but no profile yet
INSERT INTO user_profile (id, user_id, display_name, preferred_language, created_at, updated_at)
SELECT gen_random_uuid(), u.id, u.display_name, 'en', NOW(), NOW()
FROM users u
WHERE u.display_name IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_profile up WHERE up.user_id = u.id);

-- Remove display_name from users table (now in user_profile)
ALTER TABLE users DROP COLUMN display_name;
