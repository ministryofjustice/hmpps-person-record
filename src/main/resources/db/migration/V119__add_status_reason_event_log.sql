BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS event_log
    ADD COLUMN status_reason TEXT NULL;
-----------------------------------------------------
COMMIT;