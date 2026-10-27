-- V16: Drop identity_user_profile table (dead entity, replaced by user_profile in user module)
DROP TABLE IF EXISTS identity_user_profile CASCADE;
