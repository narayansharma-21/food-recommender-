ALTER TABLE menu_items
    ADD COLUMN dish_concept_id UUID;

ALTER TABLE menu_items
    ADD CONSTRAINT menu_items_dish_concept_fk
        FOREIGN KEY (dish_concept_id) REFERENCES dish_concepts (id);

CREATE INDEX menu_items_dish_concept_idx
    ON menu_items (dish_concept_id);
