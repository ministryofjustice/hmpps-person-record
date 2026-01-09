BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.prison_religion
    ALTER COLUMN cpr_religion_id DROP NOT NULL;

-----------------------------------------------------
COMMIT;