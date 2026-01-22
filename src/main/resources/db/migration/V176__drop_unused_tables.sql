BEGIN;
-------------------------------------------------------

TRUNCATE TABLE prison_nationalities;
DROP TABLE IF EXISTS personrecordservice.prison_nationalities;

TRUNCATE TABLE prison_sexual_orientation;
DROP TABLE IF EXISTS personrecordservice.prison_sexual_orientation;

TRUNCATE TABLE prison_immigration_status;
DROP TABLE IF EXISTS personrecordservice.prison_immigration_status;

TRUNCATE TABLE prison_disability_status;
DROP TABLE IF EXISTS personrecordservice.prison_disability_status;

-----------------------------------------------------
COMMIT;