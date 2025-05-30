BEGIN;
----------------------------------------

ALTER TABLE event_log
    ALTER COLUMN date_of_birth TYPE DATE USING date_of_birth::DATE,
    DROP COLUMN date_of_birth_aliases,
    ADD COLUMN date_of_birth_aliases DATE[] DEFAULT ARRAY[]::DATE[],
    DROP COLUMN sentence_dates,
    ADD COLUMN sentence_dates DATE[] DEFAULT ARRAY[]::DATE[],
    DROP COLUMN override_markers,
    ADD COLUMN exclude_override_markers BIGINT[] DEFAULT ARRAY[]::BIGINT[],
    ADD COLUMN include_override_markers BIGINT[] DEFAULT ARRAY[]::BIGINT[];

CREATE INDEX idx_event_log_uuid ON event_log(uuid);

----------------------------------------
COMMIT;