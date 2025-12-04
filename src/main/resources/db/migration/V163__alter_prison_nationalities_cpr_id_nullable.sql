BEGIN;
-------------------------------------------------------

ALTER TABLE prison_nationalities
    ALTER COLUMN cpr_nationality_id DROP NOT NULL;

-----------------------------------------------------
COMMIT;