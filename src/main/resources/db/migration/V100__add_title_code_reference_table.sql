BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS pseudonym
    DROP COLUMN title_code;

ALTER TABLE IF EXISTS pseudonym
    ADD COLUMN fk_title_code_id BIGINT NULL;

DROP TABLE IF EXISTS title_codes;
CREATE TABLE IF NOT EXISTS title_codes (
   id                                      SERIAL      PRIMARY KEY,
   code                                    TEXT        DEFAULT NULL,
   description                             TEXT        DEFAULT NULL
);

ALTER TABLE IF EXISTS pseudonym
    ADD CONSTRAINT fk_pseudonym_title_code_id
        foreign key (fk_title_code_id)
            references title_codes
            ON DELETE SET NULL;

INSERT INTO title_codes(code, description)
VALUES ('MR', 'Mr'),
       ('MRS', 'Mrs'),
       ('MISS', 'Miss'),
       ('MS', 'Ms'),
       ('MX', 'Mx'),
       ('REV', 'REVEREND'),
       ('FR', 'FATHER'),
       ('IMAM', 'IMAM'),
       ('RABBI', 'Rabbi'),
       ('BR', 'Brother'),
       ('SR', 'Sister'),
       ('DME', 'Dame'),
       ('DR', 'Dr'),
       ('LDY', 'Lady'),
       ('LRD', 'Lord'),
       ('SIR', 'Sir'),
       ('UN', 'Unknown')
;

-----------------------------------------------------
COMMIT;