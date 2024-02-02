TRUNCATE TABLE person CASCADE;
TRUNCATE TABLE offender CASCADE;
TRUNCATE TABLE defendant CASCADE;
TRUNCATE TABLE prisoner CASCADE;

INSERT INTO person
(id, person_id, created_by, created_date, last_updated_by, last_updated_date, "version")
VALUES
    (-1, 'eed4a9a4-d853-11ed-afa1-0242ac120002',  'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-2, 'd75a9374-e2a3-11ed-b5ea-0242ac120002',  'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-3, 'ddf11834-e2a3-11ed-b5ea-0242ac120002',  'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-4, 'e374e376-e2a3-11ed-b5ea-0242ac120002',  'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-5, 'bc2306f9-fdfa-4108-97f0-63561d4f0b23',  'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-6, 'c8160215-5f23-40de-be19-c3d941612727',  'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-7, '530ea72c-da15-4a8d-9961-75f5fe14fe9d',  'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-8, '31663b51-6b06-4df6-874a-24449f437c48',  'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0);

INSERT INTO defendant
(id, fk_person_id, pnc_number, crn, forename_one, forename_two, forename_three, surname, sex, date_of_birth, created_by, created_date, last_updated_by, last_updated_date, "version")
VALUES
    (-1, -1,'2001/0171310W', 'CRN1234', 'Iestyn', 'Carey', null, 'Mahoney', 'Male', '1965-06-18', 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-2, -1,'2001/0171310W', 'CRN1234', 'Iestyn', 'Carey', 'Bob', 'Mahoney', 'Male', '1965-06-18', 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-3, -2,'2001/0171310W', 'CRN1234', 'John', null, null, 'Mahoney', 'Male', '1965-06-18', 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-4, -2,'2001/0171310W', 'CRN1234', 'John', null, null, 'Mahoney', 'Male', '1965-06-18', 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-5, -4,'1981/0154257C', 'CRN12345', 'Arthur', null, null, 'MORGAN', 'Male', '1975-01-01', 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-6, -5,'2008/0056560Z', 'CRN123', 'Harry', null, null, 'Potter', 'Male', '1965-06-18', 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-7, -6,'2001/0171310W', 'CRN1234', 'Phyllis', null, null, 'Leffler', 'FEMALE', '1997-02-28', 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-8, -3,'2001/0171310W', 'CRN1234', 'Bob', null, null, 'Mortimer', 'Male', '1965-06-18', 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0);

INSERT INTO offender
(id, fk_person_id, crn, pnc_number, created_by, created_date, last_updated_by, last_updated_date, "version")
VALUES
    (-1, -1,'CRN1234', '2001/0171310W', 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-2, -3,'CRN5432',null, 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-3, -4,'CRN4444', null,'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0),
    (-4, -5,'CRN123', '2008/0056560Z', 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0);


