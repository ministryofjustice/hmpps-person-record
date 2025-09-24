BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS event_log
    ADD COLUMN master_defendant_id TEXT NULL;
-----------------------------------------------------
COMMIT;