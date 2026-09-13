CREATE TABLE user_restrictions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    restriction_type VARCHAR(20) NOT NULL,
    restriction_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT user_restrictions_user_fk
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT user_restrictions_type_check
        CHECK (restriction_type IN ('ALLERGY', 'DIETARY')),
    CONSTRAINT user_restrictions_key_check
        CHECK (restriction_key <> '' AND restriction_key = LOWER(restriction_key)),
    CONSTRAINT user_restrictions_name_check
        CHECK (display_name <> ''),
    CONSTRAINT user_restrictions_user_key_unique
        UNIQUE (user_id, restriction_type, restriction_key)
);

CREATE INDEX user_restrictions_active_idx
    ON user_restrictions (user_id, active);

CREATE TABLE user_consent_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    consent_type VARCHAR(50) NOT NULL,
    event_sequence INTEGER NOT NULL,
    granted BOOLEAN NOT NULL,
    policy_version VARCHAR(50) NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT user_consent_events_user_fk
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT user_consent_events_type_check
        CHECK (consent_type IN ('LOCATION_DATA', 'RECOMMENDATION_PERSONALIZATION')),
    CONSTRAINT user_consent_events_sequence_check
        CHECK (event_sequence > 0),
    CONSTRAINT user_consent_events_policy_check
        CHECK (policy_version <> ''),
    CONSTRAINT user_consent_events_sequence_unique
        UNIQUE (user_id, consent_type, event_sequence)
);

CREATE INDEX user_consent_events_latest_idx
    ON user_consent_events (user_id, consent_type, event_sequence DESC);
