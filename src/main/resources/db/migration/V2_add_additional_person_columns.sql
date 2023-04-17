BEGIN;

ALTER TABLE person ADD COLUMN given_name            TEXT DEFAULT NULL,
ALTER TABLE person ADD COLUMN family_name           TEXT DEFAULT NULL,
ALTER TABLE person ADD COLUMN middle_names          TEXT DEFAULT NULL,
ALTER TABLE person ADD COLUMN date_of_birth         DATE DEFAULT NULL

COMMIT;



