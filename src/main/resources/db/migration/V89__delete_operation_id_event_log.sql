BEGIN;
-------------------------------------------------------

ALTER TABLE event_log
    DROP COLUMN IF EXISTS operation_id;

-----------------------------------------------------
COMMIT;