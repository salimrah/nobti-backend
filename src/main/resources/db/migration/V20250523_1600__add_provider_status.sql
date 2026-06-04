ALTER TABLE provider ADD COLUMN status VARCHAR(20);

UPDATE provider SET status = 'ACTIVE' WHERE active = true;
UPDATE provider SET status = 'INACTIVE' WHERE active = false;

ALTER TABLE provider ALTER COLUMN status SET NOT NULL;

ALTER TABLE provider DROP COLUMN active;
