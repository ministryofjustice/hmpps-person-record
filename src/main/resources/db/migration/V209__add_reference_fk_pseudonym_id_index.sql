BEGIN;
-------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_reference_fk_pseudonym_id ON reference(fk_pseudonym_id);

-----------------------------------------------------
COMMIT;