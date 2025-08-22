BEGIN;
-------------------------------------------------------

CREATE INDEX idx_court_probation_crn ON court_probation_link_table(crn);

-----------------------------------------------------
COMMIT;