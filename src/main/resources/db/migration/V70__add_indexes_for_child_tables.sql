BEGIN;
-------------------------------------------------------
CREATE INDEX idx_reference_identifier_type ON reference(identifier_type);
CREATE INDEX idx_reference_identifier_value ON reference(identifier_value);
-----------------------------------------------------
COMMIT;