BEGIN;
----------------------------------------

CREATE INDEX idx_person_key_person_id ON personkey(person_id);

----------------------------------------
COMMIT;