CREATE TABLE menu_source_fetch_jobs (
    job_id UUID PRIMARY KEY,
    source_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT menu_source_fetch_jobs_job_fk
        FOREIGN KEY (job_id) REFERENCES background_jobs (id),
    CONSTRAINT menu_source_fetch_jobs_source_fk
        FOREIGN KEY (source_id) REFERENCES menu_sources (id)
);

CREATE INDEX menu_source_fetch_jobs_source_created_idx
    ON menu_source_fetch_jobs (source_id, created_at);
