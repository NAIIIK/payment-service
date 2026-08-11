CREATE TABLE payments (
    id              UUID PRIMARY KEY,
    sender_id       UUID NOT NULL,
    recipient_id    UUID NOT NULL,
    amount          NUMERIC(19, 4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    version         BIGINT DEFAULT 0 NOT NULL,
    created_at      TIMESTAMP NOT NULL
);