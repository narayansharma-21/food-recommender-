CREATE TABLE identity_provisioning_locks (
    lock_key INTEGER PRIMARY KEY,
    CONSTRAINT identity_provisioning_locks_key_check
        CHECK (lock_key BETWEEN 0 AND 15)
);

INSERT INTO identity_provisioning_locks (lock_key) VALUES
    (0),
    (1),
    (2),
    (3),
    (4),
    (5),
    (6),
    (7),
    (8),
    (9),
    (10),
    (11),
    (12),
    (13),
    (14),
    (15);
