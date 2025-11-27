BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.person
    ADD COLUMN IF NOT EXISTS ethnicity_code TEXT NULL;

ALTER TABLE IF EXISTS personrecordservice.pseudonym
    ADD COLUMN IF NOT EXISTS title_code TEXT NULL;

-----------------------------------------------------
COMMIT;