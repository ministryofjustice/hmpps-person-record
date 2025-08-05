BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    ADD COLUMN fk_ethnicity_code_id BIGINT NULL;

DROP TABLE IF EXISTS ethnicity_codes;
CREATE TABLE IF NOT EXISTS ethnicity_codes (
   id                                      SERIAL      PRIMARY KEY,
   code                                    TEXT        DEFAULT NULL,
   description                             TEXT        DEFAULT NULL
);

ALTER TABLE IF EXISTS person
    ADD CONSTRAINT fk_ethnicity_code_id
        foreign key (fk_ethnicity_code_id)
        references ethnicity_codes
    ON DELETE SET NULL;

INSERT INTO ethnicity_codes(code, description)
VALUES ('A1', 'Asian/Asian British : Indian'),
       ('A2', 'Asian/Asian British : Pakistani'),
       ('A3', 'Asian/Asian British : Bangladeshi'),
       ('A4', 'Asian/Asian British: Chinese'),
       ('A9', 'Asian/Asian British : Any other background'),
       ('B1', 'Black/Black British : Carribean'),
       ('B2', 'Black/Black British : African'),
       ('B9', 'Black/Black British: Any other background'),
       ('M1', 'Mixed : White and Black Carribean'),
       ('M2', 'Mixed : White and Black African'),
       ('M3', 'Mixed : White and Asian'),
       ('M9', 'Mixed : Any other background'),
       ('MERGE', 'Needs to be confirmed following merge'),
       ('NS', 'Prefer not to say'),
       ('O2', 'Other: Arab'),
       ('O9', 'Other: Any other background'),
       ('W1', 'White : Eng/Welsh/Scot/N.Irish/British'),
       ('W2', 'White: Irish'),
       ('W3', 'White: Gypsy or Irish Traveller'),
       ('W4', 'White: Gypsy or Irish Traveller'),
       ('W5', 'White: Roma'),
       ('W9', 'White : Any other background'),
       ('ETH03', 'Other (historic)'),
       ('ETH04', 'Z_Dummy Ethnicity 04'),
       ('ETH05', 'Z_Dummy Ethnicity 05'),
       ('O1', 'Chinese'),
       ('W8', 'White : Irish Traveller/Gypsy'),
       ('Z1', 'Missing (IAPS)'),
       ('UN', 'Unknown')
;

CREATE INDEX idx_fk_ethnicity_code_id ON person(fk_ethnicity_code_id);

ALTER TABLE IF EXISTS ethnicity_codes
    ADD CONSTRAINT unique_ethnicity_code UNIQUE (code);


-----------------------------------------------------
COMMIT;