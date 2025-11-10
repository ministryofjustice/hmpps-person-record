BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS prison_immigration_status
    RENAME COLUMN cpr_immigration_id TO cpr_immigration_status_id;

-------------------------------------------------------
COMMIT;