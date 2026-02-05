BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS reference
    ADD COLUMN identifier_comment TEXT NULL;

-----------------------------------------------------
<<<<<<< HEAD
COMMIT;
=======
COMMIT;
>>>>>>> 5014fc8f (CPR-1032 Clean up)
