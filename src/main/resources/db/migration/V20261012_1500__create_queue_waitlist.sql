-- Persistent waitlist: clients register interest when they cannot join a queue (closed/paused/full)
-- and are notified when the queue opens or has room. Unlike a queue membership, no position is held.
CREATE TABLE IF NOT EXISTS queue_waitlist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL,
    client_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'WAITING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    notified_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_queue_waitlist_provider ON queue_waitlist (provider_id);
CREATE INDEX IF NOT EXISTS idx_queue_waitlist_client ON queue_waitlist (client_id);

-- At most one active (WAITING/NOTIFIED) entry per client per provider.
CREATE UNIQUE INDEX IF NOT EXISTS uq_queue_waitlist_active
    ON queue_waitlist (provider_id, client_id)
    WHERE status IN ('WAITING', 'NOTIFIED');
