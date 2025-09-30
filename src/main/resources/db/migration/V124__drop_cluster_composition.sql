BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS event_log
    DROP COLUMN cluster_composition;

-----------------------------------------------------
COMMIT;