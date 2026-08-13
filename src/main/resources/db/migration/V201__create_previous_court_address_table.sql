BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS personrecordservice.previous_court_addresss
(
    id                                      SERIAL PRIMARY KEY,
    fk_person_id                            BIGINT NOT NULL,
    building_name                           TEXT NULL,
    building_number                         TEXT NULL,
    thoroughfare_name                       TEXT NULL,
    dependent_locality                      TEXT NULL,
    post_town                               TEXT NULL,
    postcode                                TEXT NULL,
    version                                 int4 NOT NULL DEFAULT 0,
    CONSTRAINT fk_person_id FOREIGN KEY (fk_person_id) REFERENCES person (id) ON DELETE CASCADE
    );
-----------------------------------------------------
COMMIT;