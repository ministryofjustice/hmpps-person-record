BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS override_marker add constraint fk_person_id_override_marker foreign key (fk_person_id) references person ON DELETE CASCADE;

-----------------------------------------------------
COMMIT;