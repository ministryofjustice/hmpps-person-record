BEGIN;
-------------------------------------------------------

UPDATE personrecordservice.person
SET religion = 'UNKN'
WHERE religion = '-1'

UPDATE personrecordservice.person
SET religion = 'TPRNTS'
WHERE religion = 'REL01'

-----------------------------------------------------
COMMIT;
