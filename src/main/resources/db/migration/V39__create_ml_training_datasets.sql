CREATE TABLE ml_training_datasets (
    id UUID PRIMARY KEY,
    schema_version VARCHAR(100) NOT NULL,
    from_inclusive TIMESTAMP WITH TIME ZONE NOT NULL,
    cutoff_exclusive TIMESTAMP WITH TIME ZONE NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    sha256 CHAR(64) NOT NULL,
    row_count INTEGER NOT NULL,
    code_version VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ml_training_datasets_schema_check
        CHECK (schema_version <> ''),
    CONSTRAINT ml_training_datasets_range_check
        CHECK (from_inclusive < cutoff_exclusive),
    CONSTRAINT ml_training_datasets_object_key_unique
        UNIQUE (object_key),
    CONSTRAINT ml_training_datasets_hash_check
        CHECK (LENGTH(sha256) = 64),
    CONSTRAINT ml_training_datasets_row_count_check
        CHECK (row_count >= 0),
    CONSTRAINT ml_training_datasets_code_version_check
        CHECK (code_version <> ''),
    CONSTRAINT ml_training_datasets_build_unique
        UNIQUE (schema_version, from_inclusive, cutoff_exclusive, code_version)
);
