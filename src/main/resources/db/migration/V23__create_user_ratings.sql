CREATE TABLE ratings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    dish_concept_id UUID,
    score SMALLINT NOT NULL,
    would_order_again BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ratings_user_fk
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ratings_menu_item_fk
        FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT ratings_dish_concept_fk
        FOREIGN KEY (dish_concept_id) REFERENCES dish_concepts (id),
    CONSTRAINT ratings_score_check
        CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT ratings_user_menu_item_unique
        UNIQUE (user_id, menu_item_id)
);

CREATE INDEX ratings_user_updated_idx
    ON ratings (user_id, updated_at);

CREATE INDEX ratings_menu_item_idx
    ON ratings (menu_item_id);

CREATE INDEX ratings_dish_concept_idx
    ON ratings (dish_concept_id);

CREATE TABLE rating_comments (
    rating_id UUID PRIMARY KEY,
    original_text VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT rating_comments_rating_fk
        FOREIGN KEY (rating_id) REFERENCES ratings (id),
    CONSTRAINT rating_comments_text_check
        CHECK (original_text <> '')
);

CREATE TABLE rating_tags (
    rating_id UUID NOT NULL,
    tag_key VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (rating_id, tag_key),
    CONSTRAINT rating_tags_rating_fk
        FOREIGN KEY (rating_id) REFERENCES ratings (id),
    CONSTRAINT rating_tags_key_check
        CHECK (tag_key <> '' AND tag_key = LOWER(tag_key))
);
