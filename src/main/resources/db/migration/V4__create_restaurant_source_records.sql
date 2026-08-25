CREATE TABLE restaurant_source_records (
    source VARCHAR(50) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    normalized_name VARCHAR(200) NOT NULL,
    address_line_1 VARCHAR(200),
    address_line_2 VARCHAR(200),
    city VARCHAR(100),
    region VARCHAR(100),
    postal_code VARCHAR(20),
    country_code CHAR(2),
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    phone VARCHAR(50),
    website_url VARCHAR(500),
    category VARCHAR(100),
    confidence NUMERIC(4, 3),
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (source, external_id),
    CONSTRAINT restaurant_source_records_coordinates_check
        CHECK (
            (latitude IS NULL AND longitude IS NULL)
            OR (
                latitude IS NOT NULL
                AND longitude IS NOT NULL
                AND latitude BETWEEN -90 AND 90
                AND longitude BETWEEN -180 AND 180
            )
        ),
    CONSTRAINT restaurant_source_records_confidence_check
        CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1)
);

CREATE INDEX restaurant_source_records_search_idx
    ON restaurant_source_records (source, normalized_name);

CREATE INDEX restaurant_source_records_city_idx
    ON restaurant_source_records (source, country_code, region, city);
