BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS personrecordservice.service_now_probation_merge_request
(
    id                                      SERIAL      PRIMARY KEY,
    person_uuid                             UUID UNIQUE NOT NULL
);

-----------------------------------------------------
COMMIT;