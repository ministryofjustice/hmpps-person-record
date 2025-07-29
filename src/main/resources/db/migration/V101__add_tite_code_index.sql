BEGIN;
-------------------------------------------------------


CREATE INDEX idx_fk_title_code_id ON pseudonym(fk_title_code_id);

ALTER TABLE IF EXISTS title_codes
    ADD CONSTRAINT unique_code UNIQUE (code);

-----------------------------------------------------
COMMIT;