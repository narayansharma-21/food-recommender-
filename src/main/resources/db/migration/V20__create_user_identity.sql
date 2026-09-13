CREATE TABLE users (
    id UUID PRIMARY KEY,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT users_status_check
        CHECK (status IN ('ACTIVE', 'DELETION_REQUESTED', 'DELETED')),
    CONSTRAINT users_deletion_check
        CHECK (
            (status <> 'DELETED' AND deleted_at IS NULL)
            OR (status = 'DELETED' AND deleted_at IS NOT NULL)
        )
);

CREATE TABLE user_auth_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_subject VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT user_auth_identities_user_fk
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT user_auth_identities_provider_check
        CHECK (provider <> '' AND provider = LOWER(provider)),
    CONSTRAINT user_auth_identities_subject_check
        CHECK (provider_subject <> ''),
    CONSTRAINT user_auth_identities_provider_subject_unique
        UNIQUE (provider, provider_subject),
    CONSTRAINT user_auth_identities_user_provider_unique
        UNIQUE (user_id, provider)
);

CREATE INDEX user_auth_identities_user_idx
    ON user_auth_identities (user_id);

CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,
    display_name VARCHAR(100),
    home_city VARCHAR(100),
    preferred_locale VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT user_profiles_user_fk
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT user_profiles_display_name_check
        CHECK (display_name IS NULL OR display_name <> ''),
    CONSTRAINT user_profiles_home_city_check
        CHECK (home_city IS NULL OR home_city <> ''),
    CONSTRAINT user_profiles_locale_check
        CHECK (preferred_locale <> '')
);
