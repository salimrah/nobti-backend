ALTER TABLE queue_member ADD COLUMN group_id UUID;
ALTER TABLE queue_member ADD COLUMN group_size INTEGER NOT NULL DEFAULT 1;
CREATE INDEX idx_queue_member_group ON queue_member(group_id);
