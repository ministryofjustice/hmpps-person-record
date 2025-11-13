BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.contact
    ADD COLUMN extension TEXT NULL;

-------------------------------------------------------
COMMIT;