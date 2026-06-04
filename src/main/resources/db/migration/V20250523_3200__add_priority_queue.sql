ALTER TABLE queue_member ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL';
CREATE INDEX idx_queue_member_priority ON queue_member(queue_id, priority, position);

ALTER TABLE provider ADD COLUMN priority_queue_enabled BOOLEAN NOT NULL DEFAULT FALSE;
