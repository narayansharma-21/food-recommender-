CREATE TABLE rating_revisions (
    id UUID PRIMARY KEY,
    rating_id UUID NOT NULL,
    revision_number INTEGER NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    score SMALLINT NOT NULL,
    would_order_again BOOLEAN,
    original_comment VARCHAR(4000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT rating_revisions_rating_fk
        FOREIGN KEY (rating_id) REFERENCES ratings (id),
    CONSTRAINT rating_revisions_number_check
        CHECK (revision_number > 0),
    CONSTRAINT rating_revisions_change_type_check
        CHECK (change_type IN ('CREATED', 'UPDATED', 'DELETED')),
    CONSTRAINT rating_revisions_score_check
        CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT rating_revisions_comment_check
        CHECK (original_comment IS NULL OR original_comment <> ''),
    CONSTRAINT rating_revisions_number_unique
        UNIQUE (rating_id, revision_number)
);

CREATE INDEX rating_revisions_rating_created_idx
    ON rating_revisions (rating_id, created_at);

CREATE TABLE rating_revision_tags (
    revision_id UUID NOT NULL,
    tag_key VARCHAR(50) NOT NULL,
    PRIMARY KEY (revision_id, tag_key),
    CONSTRAINT rating_revision_tags_revision_fk
        FOREIGN KEY (revision_id) REFERENCES rating_revisions (id),
    CONSTRAINT rating_revision_tags_key_check
        CHECK (tag_key <> '' AND tag_key = LOWER(tag_key))
);
