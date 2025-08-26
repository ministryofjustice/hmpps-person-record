BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    ADD COLUMN override_marker UUID NULL;

ALTER TABLE IF EXISTS person_key
    ADD COLUMN status_reason TEXT NULL;

DROP TABLE IF EXISTS override_scopes;
CREATE TABLE IF NOT EXISTS override_scopes
(
    id                                      SERIAL      PRIMARY KEY,
    scope                                   UUID        NOT NULL,
    confidence                              TEXT        NULL,
    actor                                   TEXT        NULL,
    version                                 int4        NOT NULL DEFAULT 0
);

DROP TABLE IF EXISTS person_override_scope;
CREATE TABLE IF NOT EXISTS person_override_scope
(
    person_id                               BIGINT      NOT NULL ,
    override_scope_id                       BIGINT      NOT NULL,
    PRIMARY KEY (person_id, override_scope_id),
    CONSTRAINT fk_person_id FOREIGN KEY (person_id) references person (id),
    CONSTRAINT fk_override_scope_id FOREIGN KEY (override_scope_id) references override_scopes (id)
);

create index idx_person_override_scope_person on person_override_scope(person_id);
create index idx_person_override_scope_override_scope on person_override_scope(override_scope_id);

-----------------------------------------------------
COMMIT;