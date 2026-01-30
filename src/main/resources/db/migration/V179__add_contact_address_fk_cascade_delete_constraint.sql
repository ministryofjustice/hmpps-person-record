BEGIN;
-------------------------------------------------------

ALTER TABLE personrecordservice.contact DROP CONSTRAINT fk_address_id;
ALTER TABLE personrecordservice.contact ADD CONSTRAINT fk_address_id FOREIGN KEY (fk_address_id) REFERENCES personrecordservice.address(id) ON DELETE CASCADE;

-----------------------------------------------------
COMMIT;
