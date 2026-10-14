BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS personrecordservice.override_scopes
    ADD COLUMN override_type TEXT NULL DEFAULT 'EXCLUDE';

-----------------------------------------------------
COMMIT;
