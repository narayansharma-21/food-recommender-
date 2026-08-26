CREATE TABLE restaurant_change_requests (
    id UUID PRIMARY KEY,
    location_id UUID NOT NULL,
    requester_reference VARCHAR(200) NOT NULL,
    request_kind VARCHAR(20) NOT NULL,
    correction_field VARCHAR(30),
    proposed_value VARCHAR(1000),
    reason VARCHAR(2000) NOT NULL,
    evidence_url VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    reviewer_reference VARCHAR(200),
    resolution_note VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT restaurant_change_requests_location_fk
        FOREIGN KEY (location_id) REFERENCES restaurant_locations (id),
    CONSTRAINT restaurant_change_requests_kind_check
        CHECK (request_kind IN ('CORRECTION', 'REMOVAL')),
    CONSTRAINT restaurant_change_requests_field_check
        CHECK (
            correction_field IS NULL
            OR correction_field IN ('DISPLAY_NAME', 'ADDRESS', 'PHONE', 'WEBSITE', 'HOURS', 'CLOSED_STATUS', 'OTHER')
        ),
    CONSTRAINT restaurant_change_requests_status_check
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT restaurant_change_requests_payload_check
        CHECK (
            (request_kind = 'CORRECTION' AND correction_field IS NOT NULL AND proposed_value IS NOT NULL)
            OR (request_kind = 'REMOVAL' AND correction_field IS NULL AND proposed_value IS NULL)
        ),
    CONSTRAINT restaurant_change_requests_review_check
        CHECK (
            (status = 'PENDING' AND reviewer_reference IS NULL AND reviewed_at IS NULL)
            OR (status <> 'PENDING' AND reviewer_reference IS NOT NULL AND reviewed_at IS NOT NULL)
        )
);

CREATE INDEX restaurant_change_requests_pending_idx
    ON restaurant_change_requests (status, created_at);

CREATE INDEX restaurant_change_requests_location_idx
    ON restaurant_change_requests (location_id, created_at);
