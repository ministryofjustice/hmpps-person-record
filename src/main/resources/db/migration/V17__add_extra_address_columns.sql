BEGIN;

-- TO BE DECIDED
ALTER table address
    ADD COLUMN address_line_six TEXT DEFAULT NULL,
    ADD COLUMN address_line_eight TEXT DEFAULT NULL;

COMMIT;