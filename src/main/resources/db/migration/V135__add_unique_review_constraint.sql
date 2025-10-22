BEGIN;
-------------------------------------------------------

CREATE UNIQUE INDEX unq_review_person_key_id_if_primary
    ON review_cluster_link (fk_person_key_id, cluster_type)
    WHERE cluster_type = 'PRIMARY';

-----------------------------------------------------
COMMIT;