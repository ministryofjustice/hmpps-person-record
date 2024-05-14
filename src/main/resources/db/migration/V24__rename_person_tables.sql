BEGIN;
-------------------------------------------------------

ALTER TABLE person_address RENAME TO address;
ALTER TABLE person_alias RENAME TO alias;
ALTER TABLE person_contact RENAME TO contact;

-----------------------------------------------------
COMMIT;