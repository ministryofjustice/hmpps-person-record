BEGIN;
create index idx_defendant_crn_pnc on defendant (crn, pnc_number);
create index idx_offender_crn_pnc on offender (crn, pnc_number);
COMMIT;