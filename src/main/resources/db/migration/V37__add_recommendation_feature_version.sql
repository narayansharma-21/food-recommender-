ALTER TABLE recommendation_requests
    ADD COLUMN feature_version VARCHAR(100);

UPDATE recommendation_requests
SET feature_version = 'no-profile-v1';

ALTER TABLE recommendation_requests
    ALTER COLUMN feature_version SET NOT NULL;

ALTER TABLE recommendation_requests
    ADD CONSTRAINT recommendation_requests_feature_version_check
        CHECK (feature_version <> '');
