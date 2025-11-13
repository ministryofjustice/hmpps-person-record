BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS prison_religion
    DROP COLUMN fk_person_id,
    ADD COLUMN prison_number TEXT NOT NULL,
    ADD COLUMN record_type TEXT NOT NULL;

-------------------------------------------------------
COMMIT;