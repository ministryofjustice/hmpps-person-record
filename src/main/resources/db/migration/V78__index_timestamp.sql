BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS person DROP COLUMN created_at;
ALTER TABLE IF EXISTS person DROP COLUMN last_modified_at;

ALTER TABLE IF EXISTS person
     ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE IF EXISTS person
     ADD COLUMN last_modified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;


CREATE INDEX idx_person_created_at ON person(created_at);
CREATE INDEX idx_person_last_modified_at ON person(last_modified_at);






-----------------------------------------------------
COMMIT;