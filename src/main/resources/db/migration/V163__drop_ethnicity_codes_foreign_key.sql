BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS personrecordservice.person
DROP COLUMN fk_ethnicity_code_id;

-----------------------------------------------------
COMMIT;