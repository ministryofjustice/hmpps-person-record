BEGIN;
-------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS pg_trgm;
ALTER TABLE personrecordservice.person ADD COLUMN date_of_birth_text text default NULL;
CREATE INDEX trgm_index_postcode ON personrecordservice.address USING gin (postcode gin_trgm_ops);
CREATE INDEX trgm_index_date_of_birth ON personrecordservice.person USING gin (date_of_birth_text gin_trgm_ops);

-------------------------------------------------------
COMMIT;