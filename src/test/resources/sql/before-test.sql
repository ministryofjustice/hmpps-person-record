TRUNCATE TABLE person;

INSERT INTO person
(person_id, pnc_number, crn, created_by, created_date, last_updated_by, last_updated_date, "version", date_of_birth, family_name, given_name, middle_names)
VALUES('eed4a9a4-d853-11ed-afa1-0242ac120002', 'PNC12345', 'CRN1234', 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0, '1965-06-18', 'Mahoney', 'Carey', null);
