BEGIN;
-------------------------------------------------------

create index idx_review_cluster_link_cluster_type on review_cluster_link(cluster_type);

-----------------------------------------------------
COMMIT;