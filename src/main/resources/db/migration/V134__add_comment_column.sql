BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS address
    ADD COLUMN comment TEXT DEFAULT NULL;
-----------------------------------------------------
COMMIT;