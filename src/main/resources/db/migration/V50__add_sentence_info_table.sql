BEGIN;
----------------------------------------
CREATE TABLE IF NOT EXISTS personrecordservice.sentence_info
(
    id                                      SERIAL      PRIMARY KEY,
    sentence_date                           DATE        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_person_id                            bigInt      NOT NULL
);
ALTER TABLE IF EXISTS sentence_info add constraint fk_person_sentence_info_id foreign key (fk_person_id) references person;

----------------------------------------
COMMIT;