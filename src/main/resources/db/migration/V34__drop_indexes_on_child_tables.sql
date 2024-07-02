BEGIN;
-------------------------------------------------------
DROP INDEX idx_address_fk_person_id;
DROP INDEX idx_alias_fk_person_id;
DROP INDEX idx_contact_fk_person_id;
-----------------------------------------------------
COMMIT;