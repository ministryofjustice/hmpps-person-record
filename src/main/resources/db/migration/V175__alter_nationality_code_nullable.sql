BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.nationalities
    ALTER COLUMN nationality_code DROP NOT NULL;
-----------------------------------------------------
COMMIT;