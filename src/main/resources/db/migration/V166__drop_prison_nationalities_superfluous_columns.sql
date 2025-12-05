BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.prison_nationalities
    DROP COLUMN cpr_nationality_id,
    DROP COLUMN start_date,
    DROP COLUMN end_date,
    DROP COLUMN create_date_time,
    DROP COLUMN create_user_id,
    DROP COLUMN create_display_name;

-----------------------------------------------------
COMMIT;