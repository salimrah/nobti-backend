-- Composite indexes for high-traffic queries
-- Addresses full table scans at 10K+ records

-- Queue member lookups by user (my queues today, active memberships)
CREATE INDEX IF NOT EXISTS idx_queue_member_user_status ON queue_member(client_id, status);

-- Provider rating queries (average calculation, rating trends)
CREATE INDEX IF NOT EXISTS idx_provider_rating_provider_created ON provider_rating(provider_id, created_at DESC);

-- Provider discovery filtering (category + active)
CREATE INDEX IF NOT EXISTS idx_provider_category_active ON provider(category, active);

-- Queue status queries with position ordering
CREATE INDEX IF NOT EXISTS idx_queue_member_queue_status_position ON queue_member(queue_id, status, position);

-- Linked accounts lookup (already has unique constraint, explicit index for query planner)
CREATE INDEX IF NOT EXISTS idx_linked_accounts_user ON linked_accounts(user_id);

-- Device FCM token lookups (push notification fan-out)
CREATE INDEX IF NOT EXISTS idx_device_fcm_token ON device(fcm_token) WHERE fcm_token IS NOT NULL;

-- Queue member client lookups (join/leave by client)
CREATE INDEX IF NOT EXISTS idx_queue_member_client ON queue_member(client_id, status);

-- Auth credential identity lookups (login by email/phone)
CREATE INDEX IF NOT EXISTS idx_auth_credential_type_identifier ON auth_credential(identity_type, identifier);

-- Provider service active filter (service catalog queries)
CREATE INDEX IF NOT EXISTS idx_provider_service_provider_active ON provider_service(provider_id, active);
