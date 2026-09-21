BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS personrecordservice.prison_address
(
    id               SERIAL PRIMARY KEY,
    fk_address_id    INTEGER   NOT NULL,
    create_date_time TIMESTAMP NULL,
    create_user_id   TEXT      NULL,
    modify_date_time TIMESTAMP NULL,
    modify_user_id   TEXT      NULL,
    CONSTRAINT fk_address_id FOREIGN KEY (fk_address_id) references personrecordservice.address (id),
    CONSTRAINT unique_fk_address_id UNIQUE (fk_address_id)
);

CREATE TABLE IF NOT EXISTS personrecordservice.prison_address_usage
(
    id                  SERIAL PRIMARY KEY,
    fk_address_usage_id INTEGER   NOT NULL,
    create_date_time    TIMESTAMP NULL,
    create_user_id      TEXT      NULL,
    modify_date_time    TIMESTAMP NULL,
    modify_user_id      TEXT      NULL,
    CONSTRAINT fk_address_id FOREIGN KEY (fk_address_usage_id) references personrecordservice.address_usage (id),
    CONSTRAINT unique_fk_address_usage_id UNIQUE (fk_address_usage_id)
);

CREATE TABLE IF NOT EXISTS personrecordservice.prison_contact
(
    id               SERIAL PRIMARY KEY,
    fk_contact_id    INTEGER   NOT NULL,
    create_date_time TIMESTAMP NULL,
    create_user_id   TEXT      NULL,
    modify_date_time TIMESTAMP NULL,
    modify_user_id   TEXT      NULL,
    CONSTRAINT fk_address_id FOREIGN KEY (fk_contact_id) references personrecordservice.contact (id),
    CONSTRAINT unique_fk_contact_id UNIQUE (fk_contact_id)
);

-----------------------------------------------------
COMMIT;
