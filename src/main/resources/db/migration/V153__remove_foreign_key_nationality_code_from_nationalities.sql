BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS nationalities
    DROP COLUMN fk_nationality_code_id;

-------------------------------------------------------
COMMIT;