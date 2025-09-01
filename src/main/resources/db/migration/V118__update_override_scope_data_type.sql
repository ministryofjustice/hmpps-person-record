BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS event_log
    DROP COLUMN IF EXISTS override_scopes,
    ADD COLUMN override_scopes UUID[] DEFAULT ARRAY[]::UUID[];

ALTER TABLE IF EXISTS override_scopes
    ADD CONSTRAINT unique_scope UNIQUE (scope);

-----------------------------------------------------
COMMIT;