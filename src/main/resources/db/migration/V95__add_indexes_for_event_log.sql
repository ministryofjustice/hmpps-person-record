BEGIN;
-------------------------------------------------------
CREATE INDEX idx_event_log_source_system_id ON event_log(source_system_id);
CREATE INDEX idx_event_log_event_timestamp ON event_log(event_timestamp);
CREATE INDEX idx_event_log_event_type ON event_log(event_type);
-----------------------------------------------------
COMMIT;