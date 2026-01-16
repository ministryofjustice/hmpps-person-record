BEGIN;
-------------------------------------------------------

ALTER TABLE personrecordservice.nationalities
    ADD COLUMN notes TEXT NULL;

-----------------------------------------------------
COMMIT;