BEGIN;
-------------------------------------------------------

CREATE INDEX idx_person_match_id ON person(match_id);

-----------------------------------------------------
COMMIT;