CREATE TABLE menu_processing_jobs (
    job_id UUID PRIMARY KEY,
    menu_version_id UUID NOT NULL,
    processing_stage VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT menu_processing_jobs_job_fk
        FOREIGN KEY (job_id) REFERENCES background_jobs (id),
    CONSTRAINT menu_processing_jobs_version_fk
        FOREIGN KEY (menu_version_id) REFERENCES menu_versions (id),
    CONSTRAINT menu_processing_jobs_stage_check
        CHECK (processing_stage IN ('EXTRACTION'))
);

CREATE INDEX menu_processing_jobs_version_created_idx
    ON menu_processing_jobs (menu_version_id, created_at);
