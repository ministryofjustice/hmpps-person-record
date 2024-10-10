BEGIN;
----------------------------------------

ALTER TABLE override_marker ALTER COLUMN marker_value TYPE BIGINT USING marker_value::bigint;

----------------------------------------
COMMIT;