CREATE TABLE onboarding_dishes (
    id UUID PRIMARY KEY,
    dish_concept_id UUID NOT NULL,
    launch_city VARCHAR(100) NOT NULL,
    prompt VARCHAR(300) NOT NULL,
    display_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT onboarding_dishes_dish_fk
        FOREIGN KEY (dish_concept_id) REFERENCES dish_concepts (id),
    CONSTRAINT onboarding_dishes_city_check
        CHECK (launch_city <> ''),
    CONSTRAINT onboarding_dishes_prompt_check
        CHECK (prompt <> ''),
    CONSTRAINT onboarding_dishes_order_check
        CHECK (display_order >= 0),
    CONSTRAINT onboarding_dishes_city_order_unique
        UNIQUE (launch_city, display_order),
    CONSTRAINT onboarding_dishes_city_dish_unique
        UNIQUE (launch_city, dish_concept_id)
);

CREATE INDEX onboarding_dishes_active_city_idx
    ON onboarding_dishes (active, launch_city, display_order);

CREATE TABLE onboarding_responses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    onboarding_dish_id UUID NOT NULL,
    preference_score SMALLINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT onboarding_responses_user_fk
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT onboarding_responses_dish_fk
        FOREIGN KEY (onboarding_dish_id) REFERENCES onboarding_dishes (id),
    CONSTRAINT onboarding_responses_score_check
        CHECK (preference_score BETWEEN 1 AND 5),
    CONSTRAINT onboarding_responses_user_dish_unique
        UNIQUE (user_id, onboarding_dish_id)
);

CREATE INDEX onboarding_responses_user_idx
    ON onboarding_responses (user_id, updated_at);
