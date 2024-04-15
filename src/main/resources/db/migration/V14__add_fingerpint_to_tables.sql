BEGIN;

ALTER table offender
    Add column fingerprint BOOLEAN NOT NULL default false;

ALTER table defendant
    Add column fingerprint BOOLEAN NOT NULL default false;

ALTER table prisoner
    Add column fingerprint BOOLEAN NOT NULL default false;

COMMIT;