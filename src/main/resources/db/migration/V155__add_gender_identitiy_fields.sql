BEGIN;
-------------------------------------------------------

ALTER TABLE personrecordservice.person
    ADD COLUMN gender_identity TEXT NULL;

ALTER TABLE personrecordservice.person
    ADD COLUMN self_described_gender_identity TEXT NULL;

-----------------------------------------------------
COMMIT;