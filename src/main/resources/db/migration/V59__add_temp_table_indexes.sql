BEGIN;
-------------------------------------------------------

CREATE INDEX idx_court_message_fk_hearing_id ON court_message(fk_hearing_id);

-------------------------------------------------------
COMMIT;