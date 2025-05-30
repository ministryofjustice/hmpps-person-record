BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS event_log
    RENAME COLUMN uuid to person_uuid;

-----------------------------------------------------
COMMIT;