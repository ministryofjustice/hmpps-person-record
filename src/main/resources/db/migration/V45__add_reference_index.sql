BEGIN;
-------------------------------------------------------

ALTER INDEX IF EXISTS idx_alias_fk_person_id RENAME TO idx_pseudonym_fk_person_id;
CREATE INDEX idx_reference_fk_person_id ON reference(fk_person_id);

-----------------------------------------------------
COMMIT;