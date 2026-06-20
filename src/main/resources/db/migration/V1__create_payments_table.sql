CREATE TABLE payments (
    id           VARCHAR(36)      NOT NULL,
    payment_date TIMESTAMP        NOT NULL,
    amount       DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (id)
);
