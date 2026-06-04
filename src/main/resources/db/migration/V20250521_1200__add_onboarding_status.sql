-- Add onboarding_status to users table (tracks OTP signup completion)
ALTER TABLE users ADD COLUMN onboarding_status VARCHAR(20) NOT NULL DEFAULT 'NEW';

-- Add onboarding_status to user_view read model
ALTER TABLE user_view ADD COLUMN onboarding_status VARCHAR(20) NOT NULL DEFAULT 'NEW';
