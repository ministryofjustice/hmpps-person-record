BEGIN;
-------------------------------------------------------


ALTER TABLE person
     ADD COLUMN created_at TIMESTAMP DEFAULT NULL;

ALTER TABLE person
     ADD COLUMN last_modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-----------------------------------------------------
COMMIT;