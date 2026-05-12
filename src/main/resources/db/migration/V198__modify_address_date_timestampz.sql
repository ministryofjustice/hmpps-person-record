BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.address
    ALTER COLUMN start_date TYPE timestamptz
    USING start_date AT TIME ZONE 'UTC';

ALTER TABLE IF EXISTS personrecordservice.address
    ALTER COLUMN end_date TYPE timestamptz
    USING end_date AT TIME ZONE 'UTC';

-----------------------------------------------------
COMMIT;