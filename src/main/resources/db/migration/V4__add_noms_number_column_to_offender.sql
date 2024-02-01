BEGIN;
ALTER table offender
    Add column prison_number TEXT NULL;
COMMIT;