BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS reference DROP CONSTRAINT fk_person_reference_id;
ALTER TABLE IF EXISTS reference add constraint fk_person_reference_id foreign key (fk_person_id) references person ON DELETE CASCADE;

DELETE FROM person WHERE source_system = 'LIBRA';

-----------------------------------------------------
COMMIT;