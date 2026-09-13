CREATE TABLE rating_moderation_cases (
    id UUID PRIMARY KEY,
    rating_id UUID NOT NULL,
    reporter_user_id UUID,
    source_type VARCHAR(20) NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    details VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT rating_moderation_cases_rating_fk
        FOREIGN KEY (rating_id) REFERENCES ratings (id),
    CONSTRAINT rating_moderation_cases_reporter_fk
        FOREIGN KEY (reporter_user_id) REFERENCES users (id),
    CONSTRAINT rating_moderation_cases_source_check
        CHECK (source_type IN ('USER_REPORT', 'AUTOMATED')),
    CONSTRAINT rating_moderation_cases_reason_check
        CHECK (reason_code <> '' AND reason_code = UPPER(reason_code)),
    CONSTRAINT rating_moderation_cases_status_check
        CHECK (status IN ('OPEN', 'DISMISSED', 'REMOVED')),
    CONSTRAINT rating_moderation_cases_resolution_check
        CHECK (
            (status = 'OPEN' AND resolved_at IS NULL)
            OR (status <> 'OPEN' AND resolved_at IS NOT NULL)
        )
);

CREATE INDEX rating_moderation_cases_status_created_idx
    ON rating_moderation_cases (status, created_at);

CREATE INDEX rating_moderation_cases_rating_idx
    ON rating_moderation_cases (rating_id);
