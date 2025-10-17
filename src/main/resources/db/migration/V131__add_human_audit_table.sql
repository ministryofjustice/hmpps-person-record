BEGIN;
-------------------------------------------------------

DROP TABLE IF EXISTS human_intervention_audit;
CREATE TABLE IF NOT EXISTS human_intervention_audit (
    id                                      SERIAL PRIMARY KEY,
    resolved_by                             TEXT NOT NULL,
    decision_rationale                      TEXT NOT NULL,
    fk_review_id                            BIGINT NOT NULL,
    CONSTRAINT fk_review_id FOREIGN KEY (fk_review_id) references review (id)
);
CREATE INDEX idx_human_intervention_audit_fk_review_id ON human_intervention_audit(fk_review_id);

-----------------------------------------------------
COMMIT;
