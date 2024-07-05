BEGIN;
-------------------------------------------------------
CREATE INDEX idx_person_fk_person_id ON person(fk_person_identifier_id);
-----------------------------------------------------
COMMIT;