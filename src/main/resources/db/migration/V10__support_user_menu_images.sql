ALTER TABLE menu_sources
    ADD COLUMN media_type VARCHAR(100);

ALTER TABLE menu_sources
    ADD COLUMN size_bytes BIGINT;

ALTER TABLE menu_sources
    ADD CONSTRAINT menu_sources_user_image_check
        CHECK (
            source_type <> 'USER_IMAGE'
            OR (
                origin_url IS NULL
                AND raw_object_key IS NOT NULL
                AND raw_object_key <> ''
                AND submitted_by_reference IS NOT NULL
                AND submitted_by_reference <> ''
                AND media_type IN ('image/jpeg', 'image/png')
                AND size_bytes IS NOT NULL
                AND size_bytes > 0
            )
        );
