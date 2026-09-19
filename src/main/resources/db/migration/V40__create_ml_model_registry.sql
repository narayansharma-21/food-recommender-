CREATE TABLE ml_model_versions (
    id UUID PRIMARY KEY,
    model_version VARCHAR(100) NOT NULL,
    model_type VARCHAR(30) NOT NULL,
    dataset_id UUID NOT NULL,
    artifact_object_key VARCHAR(500) NOT NULL,
    artifact_sha256 CHAR(64) NOT NULL,
    overall_mae NUMERIC(8, 6) NOT NULL,
    baseline_mae NUMERIC(8, 6) NOT NULL,
    metrics_json VARCHAR(8000) NOT NULL,
    code_version VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    promoted_at TIMESTAMP WITH TIME ZONE,
    retired_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ml_model_versions_version_unique UNIQUE (model_version),
    CONSTRAINT ml_model_versions_type_check
        CHECK (model_type IN ('GLOBAL_MEAN', 'BOOSTED_TREE')),
    CONSTRAINT ml_model_versions_dataset_fk
        FOREIGN KEY (dataset_id) REFERENCES ml_training_datasets (id),
    CONSTRAINT ml_model_versions_artifact_unique UNIQUE (artifact_object_key),
    CONSTRAINT ml_model_versions_hash_check CHECK (LENGTH(artifact_sha256) = 64),
    CONSTRAINT ml_model_versions_mae_check
        CHECK (overall_mae >= 0 AND baseline_mae >= 0),
    CONSTRAINT ml_model_versions_metrics_check CHECK (metrics_json <> ''),
    CONSTRAINT ml_model_versions_code_check CHECK (code_version <> ''),
    CONSTRAINT ml_model_versions_status_check
        CHECK (status IN ('CANDIDATE', 'ACTIVE', 'RETIRED'))
);

CREATE TABLE ml_model_promotions (
    id UUID PRIMARY KEY,
    from_model_id UUID,
    to_model_id UUID NOT NULL,
    promoted_by VARCHAR(200) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ml_model_promotions_from_fk
        FOREIGN KEY (from_model_id) REFERENCES ml_model_versions (id),
    CONSTRAINT ml_model_promotions_to_fk
        FOREIGN KEY (to_model_id) REFERENCES ml_model_versions (id),
    CONSTRAINT ml_model_promotions_actor_check CHECK (promoted_by <> ''),
    CONSTRAINT ml_model_promotions_reason_check CHECK (reason <> '')
);

CREATE INDEX ml_model_promotions_created_idx
    ON ml_model_promotions (created_at);
