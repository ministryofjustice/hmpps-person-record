BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS reference
    ADD COLUMN identifier_comment TEXT NULL;

-----------------------------------------------------
COMMIT;