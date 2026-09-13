CREATE TABLE menu_item_modifiers (
    id UUID PRIMARY KEY,
    menu_item_id UUID NOT NULL,
    display_name VARCHAR(300) NOT NULL,
    price_amount NUMERIC(10, 2),
    display_order INTEGER NOT NULL,
    CONSTRAINT menu_item_modifiers_item_fk
        FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT menu_item_modifiers_display_name_check
        CHECK (display_name <> ''),
    CONSTRAINT menu_item_modifiers_price_check
        CHECK (price_amount IS NULL OR price_amount >= 0),
    CONSTRAINT menu_item_modifiers_order_check
        CHECK (display_order >= 0),
    CONSTRAINT menu_item_modifiers_order_unique
        UNIQUE (menu_item_id, display_order)
);

CREATE INDEX menu_item_modifiers_item_idx
    ON menu_item_modifiers (menu_item_id);
