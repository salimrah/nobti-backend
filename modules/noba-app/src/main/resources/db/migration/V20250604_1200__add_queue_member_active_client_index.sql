CREATE UNIQUE INDEX IF NOT EXISTS uq_queue_member_active_client
    ON queue_member(queue_id, client_id)
    WHERE status IN ('WAITING', 'NOTIFIED', 'ACTIVE');
