BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS person DROP COLUMN created_at;
ALTER TABLE IF EXISTS person DROP COLUMN last_modified_at;

ALTER TABLE IF EXISTS person
     ADD COLUMN created TIMESTAMP WITH TIME ZONE DEFAULT NULL;

ALTER TABLE IF EXISTS person
     ADD COLUMN last_modified TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX idx_person_last_modified ON person(last_modified);






-----------------------------------------------------
COMMIT;