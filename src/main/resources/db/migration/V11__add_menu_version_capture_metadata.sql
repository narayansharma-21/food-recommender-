ALTER TABLE menu_versions
    ADD COLUMN raw_object_key VARCHAR(500);

ALTER TABLE menu_versions
    ADD COLUMN content_sha256 CHAR(64);

ALTER TABLE menu_versions
    ADD COLUMN media_type VARCHAR(100);

ALTER TABLE menu_versions
    ADD COLUMN size_bytes BIGINT;

ALTER TABLE menu_versions
    ADD CONSTRAINT menu_versions_capture_metadata_check
        CHECK (
            (
                raw_object_key IS NULL
                AND content_sha256 IS NULL
                AND media_type IS NULL
                AND size_bytes IS NULL
            )
            OR (
                raw_object_key IS NOT NULL
                AND raw_object_key <> ''
                AND content_sha256 IS NOT NULL
                AND LENGTH(content_sha256) = 64
                AND content_sha256 = LOWER(content_sha256)
                AND media_type IS NOT NULL
                AND media_type <> ''
                AND size_bytes IS NOT NULL
                AND size_bytes > 0
            )
        );

CREATE INDEX menu_versions_content_hash_idx
    ON menu_versions (source_id, content_sha256);
