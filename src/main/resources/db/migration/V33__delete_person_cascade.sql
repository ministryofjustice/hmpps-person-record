BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS alias DROP CONSTRAINT fk_person_alias_id;
ALTER TABLE IF EXISTS alias ADD CONSTRAINT fk_person_alias_id foreign key (fk_person_id) references person ON DELETE CASCADE;
ALTER TABLE IF EXISTS address DROP CONSTRAINT fk_person_address_id;
ALTER TABLE IF EXISTS address ADD CONSTRAINT fk_person_address_id foreign key (fk_person_id) references person ON DELETE CASCADE;
ALTER TABLE IF EXISTS contact DROP CONSTRAINT fk_person_contact_id;
ALTER TABLE IF EXISTS contact ADD CONSTRAINT fk_person_contact_id foreign key (fk_person_id) references person ON DELETE CASCADE;
-----------------------------------------------------
COMMIT;