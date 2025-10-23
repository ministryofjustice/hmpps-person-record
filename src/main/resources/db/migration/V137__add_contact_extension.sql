BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS additional_contact_information
(
    id                                      SERIAL      PRIMARY KEY,
    extension                               TEXT        NULL,
    fk_contact_id                           BIGINT      NULL,
    CONSTRAINT fk_contact_id FOREIGN KEY (fk_contact_id) references contact (id)
);
CREATE INDEX idx_additional_contact_information_fk_contact_id ON additional_contact_information(fk_contact_id);

-----------------------------------------------------
COMMIT;