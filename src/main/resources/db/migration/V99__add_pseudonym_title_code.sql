BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS pseudonym
    ADD COLUMN title_code TEXT DEFAULT NULL;
-----------------------------------------------------
COMMIT;