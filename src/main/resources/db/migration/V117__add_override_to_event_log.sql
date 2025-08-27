BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS event_log
    ADD COLUMN override_marker UUID NULL,
    ADD COLUMN override_scopes TEXT[] DEFAULT ARRAY[]::text[];

-----------------------------------------------------
COMMIT;