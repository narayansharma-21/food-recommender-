CREATE TABLE rating_trait_signals (
    id UUID PRIMARY KEY,
    revision_id UUID NOT NULL,
    trait_key VARCHAR(50) NOT NULL,
    sentiment VARCHAR(20) NOT NULL,
    confidence NUMERIC(5, 4) NOT NULL,
    evidence_text VARCHAR(500) NOT NULL,
    extractor_version VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT rating_trait_signals_revision_fk
        FOREIGN KEY (revision_id) REFERENCES rating_revisions (id),
    CONSTRAINT rating_trait_signals_key_check
        CHECK (trait_key <> '' AND trait_key = LOWER(trait_key)),
    CONSTRAINT rating_trait_signals_sentiment_check
        CHECK (sentiment IN ('POSITIVE', 'NEGATIVE')),
    CONSTRAINT rating_trait_signals_confidence_check
        CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT rating_trait_signals_evidence_check
        CHECK (evidence_text <> '' AND extractor_version <> ''),
    CONSTRAINT rating_trait_signals_revision_trait_unique
        UNIQUE (revision_id, trait_key)
);

CREATE INDEX rating_trait_signals_trait_idx
    ON rating_trait_signals (trait_key, sentiment);
