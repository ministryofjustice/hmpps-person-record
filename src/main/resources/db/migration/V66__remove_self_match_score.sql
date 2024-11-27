BEGIN;
----------------------------------------

ALTER TABLE person DROP COLUMN self_match_score;

----------------------------------------
COMMIT;