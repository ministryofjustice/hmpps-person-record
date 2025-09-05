BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.person
DROP COLUMN ethnicity;

-----------------------------------------------------
COMMIT;