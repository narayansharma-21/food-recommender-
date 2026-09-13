INSERT INTO ingredients (
    id, ingredient_key, display_name, normalized_name, created_at, updated_at
) VALUES
    ('20000000-0000-0000-0000-000000000001', 'clam', 'Clam', 'clam', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000002', 'lobster', 'Lobster', 'lobster', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000003', 'crab', 'Crab', 'crab', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000004', 'shrimp', 'Shrimp', 'shrimp', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000005', 'chicken', 'Chicken', 'chicken', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000006', 'beef', 'Beef', 'beef', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000007', 'pork', 'Pork', 'pork', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000008', 'cheese', 'Cheese', 'cheese', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000009', 'tomato', 'Tomato', 'tomato', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000010', 'potato', 'Potato', 'potato', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO dish_traits (
    id, trait_key, display_name, trait_category, created_at, updated_at
) VALUES
    ('30000000-0000-0000-0000-000000000001', 'creamy', 'Creamy', 'TEXTURE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('30000000-0000-0000-0000-000000000002', 'crispy', 'Crispy', 'TEXTURE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('30000000-0000-0000-0000-000000000003', 'spicy', 'Spicy', 'FLAVOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('30000000-0000-0000-0000-000000000004', 'smoky', 'Smoky', 'FLAVOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('30000000-0000-0000-0000-000000000005', 'sweet', 'Sweet', 'FLAVOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

CREATE TABLE menu_item_ingredient_evidence (
    id UUID PRIMARY KEY,
    menu_extraction_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    ingredient_id UUID NOT NULL,
    assertion VARCHAR(10) NOT NULL,
    evidence_type VARCHAR(20) NOT NULL,
    confidence NUMERIC(5, 4) NOT NULL,
    source_text VARCHAR(500) NOT NULL,
    extraction_method VARCHAR(100) NOT NULL,
    created_by_reference VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT menu_item_ingredient_evidence_extraction_fk
        FOREIGN KEY (menu_extraction_id) REFERENCES menu_extractions (id),
    CONSTRAINT menu_item_ingredient_evidence_item_fk
        FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT menu_item_ingredient_evidence_ingredient_fk
        FOREIGN KEY (ingredient_id) REFERENCES ingredients (id),
    CONSTRAINT menu_item_ingredient_evidence_assertion_check
        CHECK (assertion IN ('PRESENT', 'ABSENT')),
    CONSTRAINT menu_item_ingredient_evidence_type_check
        CHECK (evidence_type IN ('DECLARED', 'INFERRED', 'USER_CORRECTED')),
    CONSTRAINT menu_item_ingredient_evidence_confidence_check
        CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT menu_item_ingredient_evidence_source_check
        CHECK (source_text <> '' AND extraction_method <> ''),
    CONSTRAINT menu_item_ingredient_evidence_actor_check
        CHECK (
            (
                evidence_type = 'USER_CORRECTED'
                AND confidence = 1
                AND created_by_reference IS NOT NULL
                AND created_by_reference <> ''
            )
            OR (
                evidence_type IN ('DECLARED', 'INFERRED')
                AND created_by_reference IS NULL
            )
        ),
    CONSTRAINT menu_item_ingredient_evidence_unique
        UNIQUE (menu_extraction_id, menu_item_id, ingredient_id)
);

CREATE INDEX menu_item_ingredient_evidence_item_idx
    ON menu_item_ingredient_evidence (menu_item_id, evidence_type);

CREATE TABLE menu_item_trait_evidence (
    id UUID PRIMARY KEY,
    menu_extraction_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    trait_id UUID NOT NULL,
    assertion VARCHAR(10) NOT NULL,
    evidence_type VARCHAR(20) NOT NULL,
    confidence NUMERIC(5, 4) NOT NULL,
    source_text VARCHAR(500) NOT NULL,
    extraction_method VARCHAR(100) NOT NULL,
    created_by_reference VARCHAR(200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT menu_item_trait_evidence_extraction_fk
        FOREIGN KEY (menu_extraction_id) REFERENCES menu_extractions (id),
    CONSTRAINT menu_item_trait_evidence_item_fk
        FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT menu_item_trait_evidence_trait_fk
        FOREIGN KEY (trait_id) REFERENCES dish_traits (id),
    CONSTRAINT menu_item_trait_evidence_assertion_check
        CHECK (assertion IN ('PRESENT', 'ABSENT')),
    CONSTRAINT menu_item_trait_evidence_type_check
        CHECK (evidence_type IN ('DECLARED', 'INFERRED', 'USER_CORRECTED')),
    CONSTRAINT menu_item_trait_evidence_confidence_check
        CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT menu_item_trait_evidence_source_check
        CHECK (source_text <> '' AND extraction_method <> ''),
    CONSTRAINT menu_item_trait_evidence_actor_check
        CHECK (
            (
                evidence_type = 'USER_CORRECTED'
                AND confidence = 1
                AND created_by_reference IS NOT NULL
                AND created_by_reference <> ''
            )
            OR (
                evidence_type IN ('DECLARED', 'INFERRED')
                AND created_by_reference IS NULL
            )
        ),
    CONSTRAINT menu_item_trait_evidence_unique
        UNIQUE (menu_extraction_id, menu_item_id, trait_id)
);

CREATE INDEX menu_item_trait_evidence_item_idx
    ON menu_item_trait_evidence (menu_item_id, evidence_type);
