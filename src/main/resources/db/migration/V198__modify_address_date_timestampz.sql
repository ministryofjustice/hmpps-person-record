BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.address
    ALTER COLUMN start_date TYPE timestamptz
    USING start_date AT TIME ZONE 'Europe/London';

ALTER TABLE IF EXISTS personrecordservice.address
    ALTER COLUMN end_date TYPE timestamptz
    USING end_date AT TIME ZONE 'Europe/London';
-----------------------------------------------------
COMMIT;