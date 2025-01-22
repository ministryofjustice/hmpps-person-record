BEGIN;
-------------------------------------------------------

 ALTER TABLE person
     ADD COLUMN match_id UUID default gen_random_uuid();
-----------------------------------------------------
COMMIT;