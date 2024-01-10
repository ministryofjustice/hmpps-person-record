alter table if exists defendant_aud add column fk_person_id bigInt;
alter table if exists offender_aud add column fk_person_id bigInt;
alter table if exists prisoner_aud add column fk_person_id bigInt;