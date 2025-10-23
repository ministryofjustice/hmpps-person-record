BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS additional_contact_information
(
    id                                      SERIAL      PRIMARY KEY,
    extension                               TEXT        NULL,
    fk_contact_id                           BIGINT      NULL
);

-----------------------------------------------------
COMMIT;