BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS personrecordservice.prison_military_record
(
    id                                      BIGSERIAL   PRIMARY KEY,
    update_id                               UUID        NOT NULL DEFAULT gen_random_uuid(),
    fk_person_id                            BIGINT      NOT NULL,
    military_branch_code                    TEXT        NOT NULL,
    start_date                              DATE        NOT NULL,
    end_date                                DATE        NULL,
    war_zone_code                           TEXT        NULL,
    military_discharge_code                 TEXT        NULL,
    military_rank_code                      TEXT        NULL,
    disciplinary_action_code                TEXT        NULL,
    description                             TEXT        NULL,
    unit_number                             TEXT        NULL,
    enlistment_location                     TEXT        NULL,
    discharge_location                      TEXT        NULL,
    selective_services                      BOOLEAN     NOT NULL DEFAULT TRUE,
    service_number                          TEXT        NULL,
    create_date_time                        TIMESTAMP   NOT NULL,
    create_user_id                          TEXT        NOT NULL,
    modify_date_time                        TIMESTAMP   NULL,
    modify_user_id                          TEXT        NULL,
    CONSTRAINT fk_prison_military_record_person_id FOREIGN KEY (fk_person_id) REFERENCES personrecordservice.person (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS unq_idx_prison_military_record_fk_person_id
    ON personrecordservice.prison_military_record (fk_person_id);

CREATE UNIQUE INDEX IF NOT EXISTS unq_idx_prison_military_record_update_id
    ON personrecordservice.prison_military_record (update_id);

CREATE TABLE IF NOT EXISTS personrecordservice.prison_military_record_history
(
    id                                      BIGSERIAL   PRIMARY KEY,
    prison_number                           TEXT        NOT NULL,
    military_branch_code                    TEXT        NOT NULL,
    start_date                              DATE        NOT NULL,
    end_date                                DATE        NULL,
    war_zone_code                           TEXT        NULL,
    military_discharge_code                 TEXT        NULL,
    military_rank_code                      TEXT        NULL,
    disciplinary_action_code                TEXT        NULL,
    description                             TEXT        NULL,
    unit_number                             TEXT        NULL,
    enlistment_location                     TEXT        NULL,
    discharge_location                      TEXT        NULL,
    selective_services                      BOOLEAN     NOT NULL DEFAULT TRUE,
    service_number                          TEXT        NULL,
    create_date_time                        TIMESTAMP   NOT NULL,
    create_user_id                          TEXT        NOT NULL,
    modify_date_time                        TIMESTAMP   NULL,
    modify_user_id                          TEXT        NULL
);

CREATE INDEX IF NOT EXISTS idx_prison_military_record_history_prison_number
    ON personrecordservice.prison_military_record_history (prison_number);

-----------------------------------------------------
COMMIT;
