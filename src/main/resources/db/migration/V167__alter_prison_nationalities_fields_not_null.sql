BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.prison_nationalities
    ALTER COLUMN prison_number SET NOT NULL,
    ALTER COLUMN record_type SET NOT NULL,
    ALTER COLUMN modify_date_time SET NOT NULL,
    ALTER COLUMN modify_user_id SET NOT NULL;

-----------------------------------------------------
COMMIT;