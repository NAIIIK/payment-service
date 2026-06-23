CREATE TABLE payment_history (
    id          UUID PRIMARY KEY,
    payment_id  UUID NOT NULL REFERENCES payments(id),
    old_status  VARCHAR(20),
    new_status  VARCHAR(20) NOT NULL,
    changed_at  TIMESTAMP NOT NULL
);