BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person_key
    add constraint merged_to_itself_check check (merged_to != id);
-----------------------------------------------------
COMMIT;