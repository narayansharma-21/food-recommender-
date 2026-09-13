ALTER TABLE menu_versions
    ADD CONSTRAINT menu_versions_source_content_unique
        UNIQUE (source_id, content_sha256);
