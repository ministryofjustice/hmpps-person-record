BEGIN;
----------------------------------------

CREATE INDEX idx_merged_to_not_null ON person(merged_to) WHERE merged_to IS NOT NULL;

----------------------------------------
COMMIT;