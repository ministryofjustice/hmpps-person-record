BEGIN;
ALTER TABLE IF EXISTS offender_aud add column prison_number TEXT NULL;
COMMIT;