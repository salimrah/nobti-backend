-- V13: Drop v1 tables replaced by identity v2
-- Safe to run only after V12 data migration is complete.
-- These tables are no longer written to by any code.

DROP TABLE IF EXISTS linked_accounts CASCADE;
DROP TABLE IF EXISTS password_reset_token CASCADE;
DROP TABLE IF EXISTS email_verification CASCADE;
DROP TABLE IF EXISTS otp_verification CASCADE;
DROP TABLE IF EXISTS auth_credential CASCADE;
DROP TABLE IF EXISTS business_profile CASCADE;
DROP TABLE IF EXISTS device CASCADE;
DROP TABLE IF EXISTS user_role CASCADE;
DROP TABLE IF EXISTS user_profile CASCADE;
DROP TABLE IF EXISTS app_user CASCADE;
