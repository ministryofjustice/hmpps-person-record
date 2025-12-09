BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.prison_nationalities
    DROP COLUMN modify_display_name;

-----------------------------------------------------
COMMIT;