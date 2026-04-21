BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.person_key
    DROP CONSTRAINT merged_to_itself_check;
-----------------------------------------------------
COMMIT;