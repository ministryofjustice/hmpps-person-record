BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS person_identifier
    RENAME TO personKey;
ALTER TABLE IF EXISTS person
    RENAME COLUMN fk_person_identifier_id TO fk_person_key_id;
ALTER TABLE IF EXISTS person
DROP CONSTRAINT fk_person_identifier_person_id;
ALTER TABLE IF EXISTS person
    ADD CONSTRAINT fk_person_key_person_id foreign key (fk_person_key_id) references personKey;
-----------------------------------------------------
COMMIT;