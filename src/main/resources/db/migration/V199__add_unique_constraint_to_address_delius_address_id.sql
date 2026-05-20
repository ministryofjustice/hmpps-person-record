BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS address
    ADD CONSTRAINT unique_delius_address_id UNIQUE (delius_address_id);

-----------------------------------------------------
COMMIT;