CREATE TABLE taste_profiles (
    user_id UUID PRIMARY KEY,
    calculation_version VARCHAR(100) NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT taste_profiles_user_fk
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT taste_profiles_version_check
        CHECK (calculation_version <> '')
);

CREATE TABLE taste_profile_features (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    feature_type VARCHAR(30) NOT NULL,
    feature_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    preference_score NUMERIC(7, 6) NOT NULL,
    evidence_count INTEGER NOT NULL,
    calculation_version VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT taste_profile_features_user_fk
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT taste_profile_features_type_check
        CHECK (feature_type IN ('CUISINE', 'INGREDIENT', 'PREPARATION', 'TRAIT')),
    CONSTRAINT taste_profile_features_key_check
        CHECK (feature_key <> '' AND feature_key = LOWER(feature_key)),
    CONSTRAINT taste_profile_features_name_check
        CHECK (display_name <> ''),
    CONSTRAINT taste_profile_features_score_check
        CHECK (preference_score BETWEEN -1 AND 1),
    CONSTRAINT taste_profile_features_evidence_check
        CHECK (evidence_count > 0),
    CONSTRAINT taste_profile_features_version_check
        CHECK (calculation_version <> ''),
    CONSTRAINT taste_profile_features_user_key_unique
        UNIQUE (user_id, feature_type, feature_key)
);

CREATE INDEX taste_profile_features_user_score_idx
    ON taste_profile_features (user_id, preference_score);

CREATE TABLE taste_profile_evidence (
    feature_id UUID NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_id UUID NOT NULL,
    contribution NUMERIC(7, 6) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (feature_id, source_type, source_id),
    CONSTRAINT taste_profile_evidence_feature_fk
        FOREIGN KEY (feature_id) REFERENCES taste_profile_features (id),
    CONSTRAINT taste_profile_evidence_source_check
        CHECK (source_type IN ('ONBOARDING_RESPONSE', 'RATING_REVISION')),
    CONSTRAINT taste_profile_evidence_contribution_check
        CHECK (contribution BETWEEN -1 AND 1)
);

CREATE INDEX taste_profile_evidence_source_idx
    ON taste_profile_evidence (source_type, source_id);
