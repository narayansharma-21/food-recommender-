CREATE TABLE dish_match_reviews (
    id UUID PRIMARY KEY,
    menu_item_id UUID NOT NULL,
    suggested_dish_concept_id UUID NOT NULL,
    confidence NUMERIC(5, 4) NOT NULL,
    match_method VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reviewer_reference VARCHAR(200),
    resolution_note VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT dish_match_reviews_item_fk
        FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT dish_match_reviews_dish_fk
        FOREIGN KEY (suggested_dish_concept_id) REFERENCES dish_concepts (id),
    CONSTRAINT dish_match_reviews_confidence_check
        CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT dish_match_reviews_method_check
        CHECK (match_method <> ''),
    CONSTRAINT dish_match_reviews_status_check
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT dish_match_reviews_resolution_check
        CHECK (
            (
                status = 'PENDING'
                AND reviewer_reference IS NULL
                AND resolution_note IS NULL
                AND resolved_at IS NULL
            )
            OR (
                status IN ('APPROVED', 'REJECTED')
                AND reviewer_reference IS NOT NULL
                AND reviewer_reference <> ''
                AND resolved_at IS NOT NULL
            )
        ),
    CONSTRAINT dish_match_reviews_candidate_unique
        UNIQUE (menu_item_id, suggested_dish_concept_id, match_method)
);

CREATE INDEX dish_match_reviews_queue_idx
    ON dish_match_reviews (status, created_at);
