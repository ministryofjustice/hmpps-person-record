BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS prison_nationalities
    ALTER COLUMN cpr_nationality_id DROP NOT NULL;

-----------------------------------------------------
COMMIT;