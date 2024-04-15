BEGIN;

ALTER table offender
    Add column fingerprint BOOLEAN NULL;

ALTER table defendant
    Add column fingerprint BOOLEAN NULL;

ALTER table prisoner
    Add column fingerprint BOOLEAN NULL;

COMMIT;