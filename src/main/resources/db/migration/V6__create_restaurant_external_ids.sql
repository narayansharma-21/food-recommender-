CREATE TABLE restaurant_external_ids (
    source VARCHAR(50) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    location_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (source, external_id),
    CONSTRAINT restaurant_external_ids_location_fk
        FOREIGN KEY (location_id) REFERENCES restaurant_locations (id)
);

CREATE INDEX restaurant_external_ids_location_idx
    ON restaurant_external_ids (location_id);
