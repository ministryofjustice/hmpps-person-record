BEGIN;
-------------------------------------------------------

ALTER TABLE personrecordservice.nationalities
    ADD COLUMN nationality_code TEXT NULL;

-----------------------------------------------------
COMMIT;