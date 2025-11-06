BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS prison_nationalities
    ADD COLUMN cpr_nationality_id                      UUID        NOT NULL,
    ADD COLUMN create_date_time                        TIMESTAMP   NULL,
    ADD COLUMN create_user_id                          TEXT        NULL,
    ADD COLUMN create_display_name                     TEXT        NULL,
    ADD COLUMN modify_date_time                        TIMESTAMP   NULL,
    ADD COLUMN modify_user_id                          TEXT        NULL,
    ADD COLUMN modify_display_name                     TEXT        NULL;

CREATE INDEX idx_prison_nationalities_cpr_nationality_id ON prison_nationalities(cpr_nationality_id);

ALTER TABLE IF EXISTS prison_sexual_orientation
    ADD COLUMN cpr_sexual_orientation_id               UUID        NOT NULL,
    ADD COLUMN create_date_time                        TIMESTAMP   NULL,
    ADD COLUMN create_user_id                          TEXT        NULL,
    ADD COLUMN create_display_name                     TEXT        NULL,
    ADD COLUMN modify_date_time                        TIMESTAMP   NULL,
    ADD COLUMN modify_user_id                          TEXT        NULL,
    ADD COLUMN modify_display_name                     TEXT        NULL;

CREATE INDEX idx_prison_sexual_orientation_cpr_sexual_orientation_id ON prison_sexual_orientation(cpr_sexual_orientation_id);

ALTER TABLE IF EXISTS prison_immigration_status
    ADD COLUMN cpr_immigration_id                      UUID        NOT NULL,
    ADD COLUMN create_date_time                        TIMESTAMP   NULL,
    ADD COLUMN create_user_id                          TEXT        NULL,
    ADD COLUMN create_display_name                     TEXT        NULL,
    ADD COLUMN modify_date_time                        TIMESTAMP   NULL,
    ADD COLUMN modify_user_id                          TEXT        NULL,
    ADD COLUMN modify_display_name                     TEXT        NULL;

CREATE INDEX idx_prison_immigration_status_cpr_immigration_id ON prison_immigration_status(prison_immigration_status);

ALTER TABLE IF EXISTS prison_disability_status
    ADD COLUMN cpr_disability_id                       UUID        NOT NULL,
    ADD COLUMN create_date_time                        TIMESTAMP   NULL,
    ADD COLUMN create_user_id                          TEXT        NULL,
    ADD COLUMN create_display_name                     TEXT        NULL,
    ADD COLUMN modify_date_time                        TIMESTAMP   NULL,
    ADD COLUMN modify_user_id                          TEXT        NULL,
    ADD COLUMN modify_display_name                     TEXT        NULL;

CREATE INDEX idx_prison_disability_status_cpr_disability_id ON prison_disability_status(cpr_disability_id);

-----------------------------------------------------
COMMIT;