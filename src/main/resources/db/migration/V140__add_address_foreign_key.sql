BEGIN;
-------------------------------------------------------

ALTER TABLE personrecordservice.contact
    ADD COLUMN fk_address_id BIGINT NULL,
    ALTER COLUMN fk_person_id DROP NOT NULL;

CREATE INDEX idx_contact_fk_address_id ON contact(fk_address_id);

ALTER TABLE contact
    ADD CONSTRAINT fk_address_id FOREIGN KEY (fk_address_id) references address (id),
    ADD CONSTRAINT contact_at_least_one_reference CHECK (fk_address_id IS NOT NULL OR fk_person_id IS NOT NULL);

-----------------------------------------------------
COMMIT;