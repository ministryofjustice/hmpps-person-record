BEGIN;
-------------------------------------------------------

ALTER TABLE person
    DROP COLUMN IF EXISTS cro,
    DROP COLUMN IF EXISTS national_insurance_number,
    DROP COLUMN IF EXISTS driver_license_number,
    DROP COLUMN IF EXISTS arrest_summons_number,
    DROP COLUMN IF EXISTS pnc;

-----------------------------------------------------
COMMIT;