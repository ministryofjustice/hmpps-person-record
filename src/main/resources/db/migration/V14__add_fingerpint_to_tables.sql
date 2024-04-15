BEGIN;

ALTER table offender
    Add column fingerpint BOOLEAN NULL;

ALTER table defendant
    Add column fingerpint BOOLEAN NULL;

ALTER table prisoner
    Add column fingerpint BOOLEAN NULL;

COMMIT;