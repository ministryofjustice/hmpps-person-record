BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.reference
    ADD COLUMN fk_pseudonym_id BIGINT NULL;

ALTER TABLE personrecordservice.reference ADD CONSTRAINT fk_pseudonym_reference_id FOREIGN KEY (fk_pseudonym_id) REFERENCES personrecordservice.pseudonym(id) ON DELETE CASCADE;
-----------------------------------------------------
COMMIT;