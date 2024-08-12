BEGIN;
----------------------------------------

CREATE INDEX idx_first_name_soundex ON person (soundex(first_name));
CREATE INDEX idx_last_name_soundex ON person (soundex(last_name));

----------------------------------------
COMMIT;