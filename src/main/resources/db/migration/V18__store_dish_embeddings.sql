CREATE TABLE dish_embeddings (
    id UUID PRIMARY KEY,
    dish_concept_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    dimensions INTEGER NOT NULL,
    vector_json TEXT NOT NULL,
    input_sha256 CHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT dish_embeddings_dish_fk
        FOREIGN KEY (dish_concept_id) REFERENCES dish_concepts (id),
    CONSTRAINT dish_embeddings_provider_check
        CHECK (provider <> '' AND provider = LOWER(provider)),
    CONSTRAINT dish_embeddings_model_check
        CHECK (model_version <> ''),
    CONSTRAINT dish_embeddings_dimensions_check
        CHECK (dimensions BETWEEN 16 AND 2048),
    CONSTRAINT dish_embeddings_input_hash_check
        CHECK (CHAR_LENGTH(input_sha256) = 64 AND input_sha256 = LOWER(input_sha256)),
    CONSTRAINT dish_embeddings_content_unique
        UNIQUE (dish_concept_id, provider, model_version, input_sha256)
);

CREATE INDEX dish_embeddings_latest_idx
    ON dish_embeddings (dish_concept_id, created_at DESC);
