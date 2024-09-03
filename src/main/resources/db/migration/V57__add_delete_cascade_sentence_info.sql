BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS sentence_info DROP CONSTRAINT fk_person_sentence_info_id;
ALTER TABLE IF EXISTS sentence_info add constraint fk_person_sentence_info_id foreign key (fk_person_id) references person ON DELETE CASCADE;

-----------------------------------------------------
COMMIT;