CREATE TABLE menus (
    id UUID PRIMARY KEY,
    restaurant_location_id UUID NOT NULL,
    menu_key VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT menus_restaurant_location_fk
        FOREIGN KEY (restaurant_location_id) REFERENCES restaurant_locations (id),
    CONSTRAINT menus_menu_key_check
        CHECK (menu_key <> ''),
    CONSTRAINT menus_display_name_check
        CHECK (display_name <> ''),
    CONSTRAINT menus_location_key_unique
        UNIQUE (restaurant_location_id, menu_key)
);

CREATE TABLE menu_sources (
    id UUID PRIMARY KEY,
    menu_id UUID NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    origin_url VARCHAR(500),
    raw_object_key VARCHAR(500),
    submitted_by_reference VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT menu_sources_menu_fk
        FOREIGN KEY (menu_id) REFERENCES menus (id),
    CONSTRAINT menu_sources_type_check
        CHECK (source_type IN ('OFFICIAL_HTML', 'OFFICIAL_PDF', 'USER_IMAGE')),
    CONSTRAINT menu_sources_menu_id_unique
        UNIQUE (menu_id, id)
);

CREATE TABLE menu_versions (
    id UUID PRIMARY KEY,
    menu_id UUID NOT NULL,
    source_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    captured_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT menu_versions_menu_fk
        FOREIGN KEY (menu_id) REFERENCES menus (id),
    CONSTRAINT menu_versions_source_menu_fk
        FOREIGN KEY (menu_id, source_id) REFERENCES menu_sources (menu_id, id),
    CONSTRAINT menu_versions_number_check
        CHECK (version_number > 0),
    CONSTRAINT menu_versions_number_unique
        UNIQUE (menu_id, version_number)
);

CREATE INDEX menu_versions_source_idx
    ON menu_versions (source_id);

CREATE TABLE menu_sections (
    id UUID PRIMARY KEY,
    menu_version_id UUID NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT menu_sections_version_fk
        FOREIGN KEY (menu_version_id) REFERENCES menu_versions (id),
    CONSTRAINT menu_sections_order_check
        CHECK (display_order >= 0),
    CONSTRAINT menu_sections_display_name_check
        CHECK (display_name <> ''),
    CONSTRAINT menu_sections_order_unique
        UNIQUE (menu_version_id, display_order)
);

CREATE INDEX menu_sections_version_idx
    ON menu_sections (menu_version_id);

CREATE TABLE menu_items (
    id UUID PRIMARY KEY,
    menu_section_id UUID NOT NULL,
    display_name VARCHAR(300) NOT NULL,
    description VARCHAR(2000),
    price_amount NUMERIC(10, 2),
    price_currency CHAR(3),
    display_order INTEGER NOT NULL,
    CONSTRAINT menu_items_section_fk
        FOREIGN KEY (menu_section_id) REFERENCES menu_sections (id),
    CONSTRAINT menu_items_price_check
        CHECK (
            (price_amount IS NULL AND price_currency IS NULL)
            OR (price_amount IS NOT NULL AND price_amount >= 0 AND price_currency IS NOT NULL)
        ),
    CONSTRAINT menu_items_display_name_check
        CHECK (display_name <> ''),
    CONSTRAINT menu_items_currency_check
        CHECK (price_currency IS NULL OR price_currency = UPPER(price_currency)),
    CONSTRAINT menu_items_order_check
        CHECK (display_order >= 0),
    CONSTRAINT menu_items_order_unique
        UNIQUE (menu_section_id, display_order)
);

CREATE INDEX menu_items_section_idx
    ON menu_items (menu_section_id);
