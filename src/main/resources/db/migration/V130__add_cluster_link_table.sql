BEGIN;
-------------------------------------------------------

DROP TABLE IF EXISTS review_cluster_link;
CREATE TABLE IF NOT EXISTS review_cluster_link (
    fk_review_id                           BIGINT      NOT NULL ,
    fk_person_key_id                       BIGINT      NOT NULL,
    cluster_type                           TEXT        NOT NULL,
    PRIMARY KEY (fk_review_id, fk_person_key_id),
    CONSTRAINT fk_review_id FOREIGN KEY (fk_review_id) references review (id),
    CONSTRAINT fk_person_key_id FOREIGN KEY (fk_person_key_id) references person_key (id)
);

create index idx_review_cluster_link_review on review_cluster_link(fk_review_id);
create index idx_review_cluster_link_person_key on review_cluster_link(fk_person_key_id);

-----------------------------------------------------
COMMIT;
