CREATE TABLE admin_audit_events (
    id UUID PRIMARY KEY,
    actor_user_id UUID NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id UUID,
    reason VARCHAR(1000) NOT NULL,
    details_json VARCHAR(4000) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT admin_audit_events_actor_fk
        FOREIGN KEY (actor_user_id) REFERENCES users (id),
    CONSTRAINT admin_audit_events_action_check
        CHECK (action_type <> '' AND action_type = UPPER(action_type)),
    CONSTRAINT admin_audit_events_target_check
        CHECK (target_type <> '' AND target_type = UPPER(target_type)),
    CONSTRAINT admin_audit_events_reason_check CHECK (reason <> ''),
    CONSTRAINT admin_audit_events_details_check CHECK (details_json <> '')
);

CREATE INDEX admin_audit_events_actor_time_idx
    ON admin_audit_events (actor_user_id, occurred_at);

CREATE INDEX admin_audit_events_target_idx
    ON admin_audit_events (target_type, target_id, occurred_at);
