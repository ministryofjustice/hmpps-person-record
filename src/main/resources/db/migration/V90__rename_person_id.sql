BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personkey
    RENAME COLUMN person_id TO person_uuid;

ALTER TABLE IF EXISTS personkey
    RENAME TO person_key;
-----------------------------------------------------
COMMIT;