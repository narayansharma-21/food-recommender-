CREATE TABLE menu_extractions (
    id UUID PRIMARY KEY,
    menu_version_id UUID NOT NULL,
    revision_number INTEGER NOT NULL,
    extraction_kind VARCHAR(20) NOT NULL,
    parent_extraction_id UUID,
    ocr_provider VARCHAR(50) NOT NULL,
    ocr_provider_version VARCHAR(100) NOT NULL,
    parser_version VARCHAR(100) NOT NULL,
    ocr_result_json TEXT NOT NULL,
    structured_result_json TEXT NOT NULL,
    field_evidence_json TEXT NOT NULL,
    created_by_reference VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT menu_extractions_version_fk
        FOREIGN KEY (menu_version_id) REFERENCES menu_versions (id),
    CONSTRAINT menu_extractions_revision_check
        CHECK (revision_number > 0),
    CONSTRAINT menu_extractions_kind_check
        CHECK (extraction_kind IN ('ORIGINAL', 'CORRECTED')),
    CONSTRAINT menu_extractions_lineage_check
        CHECK (
            (
                extraction_kind = 'ORIGINAL'
                AND revision_number = 1
                AND parent_extraction_id IS NULL
                AND created_by_reference IS NULL
            )
            OR (
                extraction_kind = 'CORRECTED'
                AND revision_number > 1
                AND parent_extraction_id IS NOT NULL
                AND created_by_reference IS NOT NULL
                AND created_by_reference <> ''
            )
        ),
    CONSTRAINT menu_extractions_version_revision_unique
        UNIQUE (menu_version_id, revision_number),
    CONSTRAINT menu_extractions_version_id_unique
        UNIQUE (menu_version_id, id),
    CONSTRAINT menu_extractions_parent_same_version_fk
        FOREIGN KEY (menu_version_id, parent_extraction_id)
        REFERENCES menu_extractions (menu_version_id, id)
);

CREATE INDEX menu_extractions_version_created_idx
    ON menu_extractions (menu_version_id, created_at);
