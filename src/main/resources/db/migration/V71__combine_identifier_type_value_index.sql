BEGIN;
-------------------------------------------------------

DROP INDEX idx_reference_identifier_type;
DROP INDEX idx_reference_identifier_value;

CREATE INDEX idx_reference_identifier_type_and_value ON reference(identifier_type,identifier_value);

-----------------------------------------------------
COMMIT;