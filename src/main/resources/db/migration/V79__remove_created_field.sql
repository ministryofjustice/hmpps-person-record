BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS person DROP COLUMN created;
ALTER TABLE IF EXISTS person DROP COLUMN last_modified;

ALTER TABLE IF EXISTS person
     ADD COLUMN last_modified TIMESTAMP WITH TIME ZONE DEFAULT NULL;



-----------------------------------------------------
COMMIT;