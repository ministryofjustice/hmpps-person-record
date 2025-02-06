BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person ADD COLUMN c_id TEXT;

ALTER TABLE IF EXISTS person ADD CONSTRAINT unique_c_id UNIQUE (c_id);
-----------------------------------------------------
COMMIT;