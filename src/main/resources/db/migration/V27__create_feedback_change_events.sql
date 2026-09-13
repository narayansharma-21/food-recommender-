CREATE TABLE feedback_change_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    rating_id UUID NOT NULL,
    revision_id UUID NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT feedback_change_events_user_fk
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT feedback_change_events_rating_fk
        FOREIGN KEY (rating_id) REFERENCES ratings (id),
    CONSTRAINT feedback_change_events_revision_fk
        FOREIGN KEY (revision_id) REFERENCES rating_revisions (id),
    CONSTRAINT feedback_change_events_type_check
        CHECK (change_type IN ('CREATED', 'UPDATED', 'DELETED')),
    CONSTRAINT feedback_change_events_revision_unique
        UNIQUE (revision_id)
);

CREATE INDEX feedback_change_events_pending_idx
    ON feedback_change_events (processed_at, occurred_at);
