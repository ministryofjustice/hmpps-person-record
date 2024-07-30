BEGIN;
-------------------------------------------------------

ALTER TABLE person ADD COLUMN self_match_score NUMERIC NULL;

-----------------------------------------------------
COMMIT;