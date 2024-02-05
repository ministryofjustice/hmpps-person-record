BEGIN;
ALTER TABLE IF EXISTS prisoner RENAME COLUMN offender_id TO prison_number;
ALTER TABLE IF EXISTS prisoner_aud RENAME COLUMN offender_id TO prison_number;
COMMIT;