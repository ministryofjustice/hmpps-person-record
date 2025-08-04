BEGIN;
-------------------------------------------------------

DROP TABLE IF EXISTS nationalities;
CREATE TABLE IF NOT EXISTS nationalities
(
    id                                      SERIAL      PRIMARY KEY,
    fk_nationality_code_id                     BIGINT      NOT NULL,
    start_date                              DATE        NULL,
    end_date                                DATE        NULL,
    notes                                   TEXT        NULL,
    fk_person_id                            BIGINT      NOT NULL
);
ALTER TABLE IF EXISTS nationalities add constraint fk_person_nationalities_id foreign key (fk_person_id) references person;

CREATE INDEX idx_nationalities_fk_person_id ON nationalities(fk_person_id);
CREATE INDEX idx_nationality_code_id ON nationalities(fk_nationality_code_id);

DROP TABLE IF EXISTS nationality_codes;
CREATE TABLE IF NOT EXISTS nationality_codes
(
    id                                      SERIAL      PRIMARY KEY,
    code                                    TEXT        NULL,
    description                             TEXT        NULL
);

ALTER TABLE IF EXISTS nationality_codes
    ADD CONSTRAINT unique_nationality_code UNIQUE (code);

ALTER TABLE IF EXISTS nationalities
    ADD CONSTRAINT fk_nationalities_nationality_code_id
        foreign key (fk_nationality_code_id)
            references nationality_codes
            ON DELETE SET NULL;

-----------------------------------------------------
COMMIT;