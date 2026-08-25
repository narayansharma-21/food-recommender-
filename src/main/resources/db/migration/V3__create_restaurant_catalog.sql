CREATE TABLE restaurants (
    id UUID PRIMARY KEY,
    display_name VARCHAR(200) NOT NULL,
    normalized_name VARCHAR(200) NOT NULL,
    website_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT restaurants_normalized_name_check
        CHECK (normalized_name <> '')
);

CREATE INDEX restaurants_normalized_name_idx
    ON restaurants (normalized_name);

CREATE TABLE restaurant_locations (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL,
    address_line_1 VARCHAR(200) NOT NULL,
    address_line_2 VARCHAR(200),
    city VARCHAR(100) NOT NULL,
    region VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    country_code CHAR(2) NOT NULL,
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    phone_e164 VARCHAR(16),
    timezone VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT restaurant_locations_restaurant_fk
        FOREIGN KEY (restaurant_id) REFERENCES restaurants (id),
    CONSTRAINT restaurant_locations_coordinates_check
        CHECK (
            (latitude IS NULL AND longitude IS NULL)
            OR (
                latitude IS NOT NULL
                AND longitude IS NOT NULL
                AND latitude BETWEEN -90 AND 90
                AND longitude BETWEEN -180 AND 180
            )
        ),
    CONSTRAINT restaurant_locations_status_check
        CHECK (status IN ('ACTIVE', 'CLOSED', 'REMOVED'))
);

CREATE INDEX restaurant_locations_restaurant_idx
    ON restaurant_locations (restaurant_id);

CREATE INDEX restaurant_locations_city_idx
    ON restaurant_locations (country_code, region, city);

CREATE INDEX restaurant_locations_coordinates_idx
    ON restaurant_locations (latitude, longitude);
