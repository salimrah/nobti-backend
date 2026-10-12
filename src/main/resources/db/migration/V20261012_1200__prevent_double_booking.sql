-- Prevent double-booking at the database level.
--
-- The application performs a check-then-insert (existsOverlapping) which two concurrent requests
-- can both pass. A GiST exclusion constraint on (provider_id, time range) makes overlapping,
-- non-cancelled appointments impossible regardless of concurrency; the application maps the
-- resulting constraint violation to HTTP 409.
--
-- A cancelled appointment frees the slot, so only rows with status <> 'CANCELLED' are considered.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE appointment
    ADD CONSTRAINT ex_appointment_provider_slot
        EXCLUDE USING gist (
            provider_id WITH =,
            tsrange(start_time, end_time) WITH &&
        )
        WHERE (status <> 'CANCELLED');
