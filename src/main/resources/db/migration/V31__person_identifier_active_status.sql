BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS person_identifier
    ADD COLUMN status TEXT DEFAULT 'ACTIVE';
-----------------------------------------------------
COMMIT;