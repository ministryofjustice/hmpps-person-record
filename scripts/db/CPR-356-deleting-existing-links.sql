-------------------------------------------------------
-- These commands should only be run as part of the process
-- to repopulate links data
-- Be careful
-------------------------------------------------------

-- delete all foreign key references from person table
update personrecordservice.person
set fk_person_identifier_id = null;

-- then empty the person_identifier table
delete from personrecordservice.person_identifier

