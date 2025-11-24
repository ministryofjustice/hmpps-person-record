BEGIN;
-------------------------------------------------------

ALTER TABLE personrecordservice.person
    ADD COLUMN ethnicity_code TEXT NULL;

-----------------------------------------------------
COMMIT;