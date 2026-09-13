CREATE TABLE dish_concepts (
    id UUID PRIMARY KEY,
    concept_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    normalized_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT dish_concepts_key_check
        CHECK (concept_key <> '' AND concept_key = LOWER(concept_key)),
    CONSTRAINT dish_concepts_name_check
        CHECK (display_name <> '' AND normalized_name <> ''),
    CONSTRAINT dish_concepts_key_unique
        UNIQUE (concept_key)
);

CREATE INDEX dish_concepts_normalized_name_idx
    ON dish_concepts (normalized_name);

CREATE TABLE ingredients (
    id UUID PRIMARY KEY,
    ingredient_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    normalized_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ingredients_key_check
        CHECK (ingredient_key <> '' AND ingredient_key = LOWER(ingredient_key)),
    CONSTRAINT ingredients_name_check
        CHECK (display_name <> '' AND normalized_name <> ''),
    CONSTRAINT ingredients_key_unique
        UNIQUE (ingredient_key)
);

CREATE INDEX ingredients_normalized_name_idx
    ON ingredients (normalized_name);

CREATE TABLE cuisines (
    id UUID PRIMARY KEY,
    cuisine_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT cuisines_key_check
        CHECK (cuisine_key <> '' AND cuisine_key = LOWER(cuisine_key)),
    CONSTRAINT cuisines_display_name_check
        CHECK (display_name <> ''),
    CONSTRAINT cuisines_key_unique
        UNIQUE (cuisine_key)
);

CREATE TABLE preparations (
    id UUID PRIMARY KEY,
    preparation_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT preparations_key_check
        CHECK (preparation_key <> '' AND preparation_key = LOWER(preparation_key)),
    CONSTRAINT preparations_display_name_check
        CHECK (display_name <> ''),
    CONSTRAINT preparations_key_unique
        UNIQUE (preparation_key)
);

CREATE TABLE dish_traits (
    id UUID PRIMARY KEY,
    trait_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    trait_category VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT dish_traits_key_check
        CHECK (trait_key <> '' AND trait_key = LOWER(trait_key)),
    CONSTRAINT dish_traits_fields_check
        CHECK (display_name <> '' AND trait_category <> ''),
    CONSTRAINT dish_traits_key_unique
        UNIQUE (trait_key)
);

CREATE INDEX dish_traits_category_idx
    ON dish_traits (trait_category);

CREATE TABLE dish_concept_ingredients (
    dish_concept_id UUID NOT NULL,
    ingredient_id UUID NOT NULL,
    PRIMARY KEY (dish_concept_id, ingredient_id),
    CONSTRAINT dish_concept_ingredients_dish_fk
        FOREIGN KEY (dish_concept_id) REFERENCES dish_concepts (id),
    CONSTRAINT dish_concept_ingredients_ingredient_fk
        FOREIGN KEY (ingredient_id) REFERENCES ingredients (id)
);

CREATE TABLE dish_concept_cuisines (
    dish_concept_id UUID NOT NULL,
    cuisine_id UUID NOT NULL,
    PRIMARY KEY (dish_concept_id, cuisine_id),
    CONSTRAINT dish_concept_cuisines_dish_fk
        FOREIGN KEY (dish_concept_id) REFERENCES dish_concepts (id),
    CONSTRAINT dish_concept_cuisines_cuisine_fk
        FOREIGN KEY (cuisine_id) REFERENCES cuisines (id)
);

CREATE TABLE dish_concept_preparations (
    dish_concept_id UUID NOT NULL,
    preparation_id UUID NOT NULL,
    PRIMARY KEY (dish_concept_id, preparation_id),
    CONSTRAINT dish_concept_preparations_dish_fk
        FOREIGN KEY (dish_concept_id) REFERENCES dish_concepts (id),
    CONSTRAINT dish_concept_preparations_preparation_fk
        FOREIGN KEY (preparation_id) REFERENCES preparations (id)
);

CREATE TABLE dish_concept_traits (
    dish_concept_id UUID NOT NULL,
    trait_id UUID NOT NULL,
    PRIMARY KEY (dish_concept_id, trait_id),
    CONSTRAINT dish_concept_traits_dish_fk
        FOREIGN KEY (dish_concept_id) REFERENCES dish_concepts (id),
    CONSTRAINT dish_concept_traits_trait_fk
        FOREIGN KEY (trait_id) REFERENCES dish_traits (id)
);
