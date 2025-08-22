BEGIN;
-------------------------------------------------------

CREATE INDEX idx_ethnicity ON person(ethnicity);

-----------------------------------------------------
COMMIT;