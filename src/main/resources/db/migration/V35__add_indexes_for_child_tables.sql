BEGIN;
-------------------------------------------------------
CREATE INDEX idx_address_fk_person_id ON address(fk_person_id);
CREATE INDEX idx_alias_fk_person_id ON alias(fk_person_id);
CREATE INDEX idx_contact_fk_person_id ON contact(fk_person_id);
-----------------------------------------------------
COMMIT;