BEGIN;
----------------------------------------

ALTER TABLE event_log
    ALTER COLUMN date_of_birth TYPE DATE USING date_of_birth::DATE,
    DROP COLUMN date_of_birth_aliases,
    ADD COLUMN date_of_birth_aliases DATE[] DEFAULT ARRAY[]::DATE[],
    DROP COLUMN sentence_dates,
    ADD COLUMN sentence_dates DATE[] DEFAULT ARRAY[]::DATE[];

CREATE INDEX idx_event_log_uuid ON event_log(uuid);

----------------------------------------
COMMIT;