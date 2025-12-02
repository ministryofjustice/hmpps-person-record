BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS pseudonym
    DROP COLUMN fk_title_code_id;

-------------------------------------------------------
COMMIT;