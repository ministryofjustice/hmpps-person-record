BEGIN;

ALTER table defendant
    ALTER column fk_person_id DROP NOT NULL;

ALTER table offender
    ALTER column fk_person_id DROP NOT NULL;

ALTER table prisoner
    ALTER column fk_person_id DROP NOT NULL;

COMMIT;