BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS person_identifier RENAME TO personKey;
ALTER TABLE IF EXISTS person RENAME COLUMN fk_person_identifier_id TO fk_person_key_id;
-----------------------------------------------------
COMMIT;