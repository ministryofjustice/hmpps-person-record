BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS personrecordservice.prison_reference
(
    id                                      SERIAL      PRIMARY KEY,
    update_id                               UUID        DEFAULT gen_random_uuid() NOT NULL,
    fk_pseudonym_id                         BIGINT      NOT NULL,
    identifier_type                         TEXT        DEFAULT NULL,
    identifier_value                        TEXT        DEFAULT NULL,
    identifier_comment                      TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0
);
ALTER TABLE IF EXISTS prison_reference add constraint fk_pseudonym_reference_id foreign key (fk_pseudonym_id) references pseudonym;

-----------------------------------------------------
COMMIT;