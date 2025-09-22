BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.event_log
DROP COLUMN exclude_override_markers;

ALTER TABLE IF EXISTS personrecordservice.event_log
DROP COLUMN include_override_markers;
-----------------------------------------------------
COMMIT;