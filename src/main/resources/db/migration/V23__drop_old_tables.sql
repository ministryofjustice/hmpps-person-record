BEGIN;
-------------------------------------------------------

TRUNCATE TABLE person_identifier CASCADE;
DROP TABLE IF EXISTS address CASCADE;
DROP TABLE IF EXISTS contact CASCADE;
DROP TABLE IF EXISTS offender_alias CASCADE;
DROP TABLE IF EXISTS offender CASCADE;
DROP TABLE IF EXISTS defendant_alias CASCADE;
DROP TABLE IF EXISTS defendant CASCADE;
DROP TABLE IF EXISTS prisoner_alias CASCADE;
DROP TABLE IF EXISTS prisoner CASCADE;

-----------------------------------------------------
COMMIT;