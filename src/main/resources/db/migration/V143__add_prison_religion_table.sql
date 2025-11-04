BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS prison_religion
(
    id                                      SERIAL      PRIMARY KEY,
    cpr_religion_id                         UUID        NOT NULL,
    religion_code                           TEXT        NULL,
    status                                  TEXT        NULL,
    start_date                              DATE        NULL,
    end_date                                DATE        NULL,
    change_reason_known                     TEXT        NULL,
    comments                                TEXT        NULL,
    verified                                BOOL        NULL,
    create_date_time                        TIMESTAMP   NULL,
    create_user_id                          TEXT        NULL,
    create_display_name                     TEXT        NULL,
    modify_date_time                        TIMESTAMP   NULL,
    modify_user_id                          TEXT        NULL,
    modify_display_name                     TEXT        NULL,
    fk_person_id                            BIGINT      NULL,
    CONSTRAINT fk_person_id FOREIGN KEY (fk_person_id) references person (id)
);

CREATE INDEX idx_prison_religion_fk_person_id ON prison_religion(fk_person_id);

-----------------------------------------------------
COMMIT;