BEGIN;
-------------------------------------------------------

CREATE INDEX idx_sentence_info_fk_person_id ON sentence_info(fk_person_id);

-----------------------------------------------------
COMMIT;