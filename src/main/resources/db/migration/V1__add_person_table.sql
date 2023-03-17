DROP TABLE IF EXISTS PERSON;

CREATE TABLE IF NOT EXISTS PERSON
(
    ID                                     SERIAL       PRIMARY KEY,
    PERSON_ID                              UUID         NOT NULL,
    PNC                                    TEXT         DEFAULT NULL,
    CRN                                    TEXT         DEFAULT NULL
);