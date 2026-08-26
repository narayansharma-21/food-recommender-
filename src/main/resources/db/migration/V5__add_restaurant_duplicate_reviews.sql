ALTER TABLE restaurants
    ADD COLUMN merged_into_id UUID;

ALTER TABLE restaurants
    ADD CONSTRAINT restaurants_merged_into_fk
        FOREIGN KEY (merged_into_id) REFERENCES restaurants (id);

ALTER TABLE restaurants
    ADD CONSTRAINT restaurants_not_merged_into_self_check
        CHECK (merged_into_id IS NULL OR merged_into_id <> id);

ALTER TABLE restaurant_locations
    ADD COLUMN merged_into_id UUID;

ALTER TABLE restaurant_locations
    ADD CONSTRAINT restaurant_locations_merged_into_fk
        FOREIGN KEY (merged_into_id) REFERENCES restaurant_locations (id);

ALTER TABLE restaurant_locations
    ADD CONSTRAINT restaurant_locations_not_merged_into_self_check
        CHECK (merged_into_id IS NULL OR merged_into_id <> id);

CREATE TABLE restaurant_duplicate_reviews (
    id UUID PRIMARY KEY,
    location_a_id UUID NOT NULL,
    location_b_id UUID NOT NULL,
    match_score NUMERIC(4, 3) NOT NULL,
    reasons_text VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    canonical_location_id UUID,
    reviewer_reference VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT restaurant_duplicate_reviews_location_a_fk
        FOREIGN KEY (location_a_id) REFERENCES restaurant_locations (id),
    CONSTRAINT restaurant_duplicate_reviews_location_b_fk
        FOREIGN KEY (location_b_id) REFERENCES restaurant_locations (id),
    CONSTRAINT restaurant_duplicate_reviews_canonical_location_fk
        FOREIGN KEY (canonical_location_id) REFERENCES restaurant_locations (id),
    CONSTRAINT restaurant_duplicate_reviews_distinct_locations_check
        CHECK (location_a_id <> location_b_id),
    CONSTRAINT restaurant_duplicate_reviews_score_check
        CHECK (match_score BETWEEN 0 AND 1),
    CONSTRAINT restaurant_duplicate_reviews_status_check
        CHECK (status IN ('PENDING', 'MERGED', 'DISMISSED')),
    CONSTRAINT restaurant_duplicate_reviews_decision_check
        CHECK (
            (status = 'PENDING' AND canonical_location_id IS NULL AND reviewed_at IS NULL)
            OR (status = 'DISMISSED' AND canonical_location_id IS NULL AND reviewed_at IS NOT NULL)
            OR (
                status = 'MERGED'
                AND canonical_location_id IN (location_a_id, location_b_id)
                AND reviewed_at IS NOT NULL
            )
        ),
    CONSTRAINT restaurant_duplicate_reviews_pair_unique
        UNIQUE (location_a_id, location_b_id)
);

CREATE INDEX restaurant_duplicate_reviews_status_idx
    ON restaurant_duplicate_reviews (status, created_at);
