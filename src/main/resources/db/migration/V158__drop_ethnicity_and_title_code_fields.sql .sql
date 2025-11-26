BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.person
    DROP COLUMN ethnicity_code;

ALTER TABLE IF EXISTS personrecordservice.pseudonym
    DROP COLUMN title_code;

-----------------------------------------------------
COMMIT;
