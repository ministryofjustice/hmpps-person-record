BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS personrecordservice.prison_pseudonym
(
    id                                      SERIAL      PRIMARY KEY,
    fk_pseudonym_id                         BIGINT      NOT NULL,
    suffix                                  TEXT        DEFAULT NULL,
    alias_name_type                         TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0
    );
ALTER TABLE IF EXISTS prison_pseudonym add constraint fk_pseudonym_prison_pseudonym_id foreign key (fk_pseudonym_id) references pseudonym ON DELETE CASCADE;

-----------------------------------------------------
COMMIT;