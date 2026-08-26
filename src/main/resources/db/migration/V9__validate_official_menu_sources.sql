ALTER TABLE menu_sources
    ADD CONSTRAINT menu_sources_official_url_check
        CHECK (
            (source_type IN ('OFFICIAL_HTML', 'OFFICIAL_PDF') AND origin_url IS NOT NULL AND origin_url <> '')
            OR source_type = 'USER_IMAGE'
        );

ALTER TABLE menu_sources
    ADD CONSTRAINT menu_sources_official_url_unique
        UNIQUE (menu_id, source_type, origin_url);
