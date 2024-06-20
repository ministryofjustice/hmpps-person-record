BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS person
    ADD COLUMN fk_person_identifier_id int DEFAULT NULL;
ALTER TABLE IF EXISTS person
    ADD CONSTRAINT fk_person_identifier_person_id foreign key (fk_person_identifier_id) references person_identifier;
-----------------------------------------------------
COMMIT;