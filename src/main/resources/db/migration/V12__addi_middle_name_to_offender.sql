BEGIN;

ALTER table offender
    Add column middle_name TEXT NULL;

COMMIT;