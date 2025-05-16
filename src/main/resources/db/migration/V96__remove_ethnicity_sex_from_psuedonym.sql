BEGIN;
-------------------------------------------------------

ALTER TABLE personrecordservice.pseudonym
    DROP COLUMN IF EXISTS sex,
    DROP COLUMN IF EXISTS ethnicity;

-----------------------------------------------------
COMMIT;