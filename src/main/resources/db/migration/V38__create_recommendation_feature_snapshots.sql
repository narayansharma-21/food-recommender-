CREATE TABLE recommendation_result_features (
    result_id UUID PRIMARY KEY,
    personal_preference NUMERIC(7, 6) NOT NULL,
    popularity NUMERIC(7, 6) NOT NULL,
    taste_evidence_count INTEGER NOT NULL,
    popularity_rating_count INTEGER NOT NULL,
    previously_rated BOOLEAN NOT NULL,
    CONSTRAINT recommendation_result_features_result_fk
        FOREIGN KEY (result_id) REFERENCES recommendation_results (id),
    CONSTRAINT recommendation_result_features_preference_check
        CHECK (personal_preference BETWEEN -1 AND 1),
    CONSTRAINT recommendation_result_features_popularity_check
        CHECK (popularity BETWEEN 0 AND 1),
    CONSTRAINT recommendation_result_features_taste_evidence_check
        CHECK (taste_evidence_count >= 0),
    CONSTRAINT recommendation_result_features_popularity_evidence_check
        CHECK (popularity_rating_count >= 0)
);
