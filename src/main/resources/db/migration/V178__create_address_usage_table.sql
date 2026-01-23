BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS personrecordservice.address_usage
(
    id                                      SERIAL      PRIMARY KEY,
    fk_address_id                           BIGINT      NOT NULL,
    usage_code                              TEXT        NOT NULL,
    active                                  BOOLEAN     NOT NULL,
    CONSTRAINT fk_address_id FOREIGN KEY (fk_address_id) REFERENCES address (id) ON DELETE CASCADE
);
CREATE index idx_address_usage_fk_address_id ON address_usage(fk_address_id);

-----------------------------------------------------
COMMIT;