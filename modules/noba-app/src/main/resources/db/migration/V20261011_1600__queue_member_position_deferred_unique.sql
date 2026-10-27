-- Position renumbering (priority inserts, advancing the queue) temporarily produces duplicate
-- (queue_id, position) values within a single transaction: Hibernate flushes inserts before
-- updates, and reordering can swap positions between two rows. A per-statement unique constraint
-- therefore rejects otherwise-valid operations (e.g. joining with EMERGENCY priority, or
-- advancing the queue) with a 23505 error.
--
-- Deferring the constraint moves validation to COMMIT, so the same guarantees hold (the final
-- state must still be unique) while allowing transient duplicates during renumbering.
ALTER TABLE queue_member
    DROP CONSTRAINT IF EXISTS uq_queue_member_position;

ALTER TABLE queue_member
    ADD CONSTRAINT uq_queue_member_position
        UNIQUE (queue_id, position) DEFERRABLE INITIALLY DEFERRED;
