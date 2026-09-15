CREATE TABLE recommendation_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    restaurant_id UUID NOT NULL,
    menu_version_id UUID NOT NULL,
    mode VARCHAR(30) NOT NULL,
    algorithm_version VARCHAR(100) NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT recommendation_requests_user_fk
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT recommendation_requests_restaurant_fk
        FOREIGN KEY (restaurant_id) REFERENCES restaurants (id),
    CONSTRAINT recommendation_requests_menu_version_fk
        FOREIGN KEY (menu_version_id) REFERENCES menu_versions (id),
    CONSTRAINT recommendation_requests_mode_check
        CHECK (mode IN ('SAFE_BET', 'TRY_SOMETHING_NEW')),
    CONSTRAINT recommendation_requests_version_check
        CHECK (algorithm_version <> '')
);

CREATE INDEX recommendation_requests_user_generated_idx
    ON recommendation_requests (user_id, generated_at);

CREATE TABLE recommendation_results (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    result_rank INTEGER NOT NULL,
    score NUMERIC(9, 6) NOT NULL,
    confidence VARCHAR(10) NOT NULL,
    CONSTRAINT recommendation_results_request_fk
        FOREIGN KEY (request_id) REFERENCES recommendation_requests (id),
    CONSTRAINT recommendation_results_menu_item_fk
        FOREIGN KEY (menu_item_id) REFERENCES menu_items (id),
    CONSTRAINT recommendation_results_rank_check
        CHECK (result_rank > 0),
    CONSTRAINT recommendation_results_confidence_check
        CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT recommendation_results_request_rank_unique
        UNIQUE (request_id, result_rank),
    CONSTRAINT recommendation_results_request_item_unique
        UNIQUE (request_id, menu_item_id)
);

CREATE TABLE recommendation_reasons (
    result_id UUID NOT NULL,
    reason_order INTEGER NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    explanation VARCHAR(300) NOT NULL,
    PRIMARY KEY (result_id, reason_order),
    CONSTRAINT recommendation_reasons_result_fk
        FOREIGN KEY (result_id) REFERENCES recommendation_results (id),
    CONSTRAINT recommendation_reasons_order_check
        CHECK (reason_order >= 0),
    CONSTRAINT recommendation_reasons_code_check
        CHECK (reason_code <> '' AND reason_code = UPPER(reason_code)),
    CONSTRAINT recommendation_reasons_explanation_check
        CHECK (explanation <> '')
);
