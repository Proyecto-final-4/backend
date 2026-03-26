CREATE TABLE transactions (
    id               UUID           NOT NULL DEFAULT gen_random_uuid(),
    user_id          UUID           NOT NULL,
    category_id      UUID           NOT NULL,
    amount           DECIMAL(19, 2) NOT NULL,
    type             VARCHAR(20)    NOT NULL,
    transaction_date DATE           NOT NULL,
    description      VARCHAR(500)   NOT NULL,
    notes            VARCHAR(500),
    embedding        vector(1536),
    created_at       TIMESTAMP      NOT NULL,
    updated_at       TIMESTAMP      NOT NULL,
    CONSTRAINT pk_transactions             PRIMARY KEY (id),
    CONSTRAINT fk_transactions_user        FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT chk_transactions_type       CHECK (type IN ('INCOME', 'EXPENSE'))
);

CREATE INDEX idx_transactions_user_id          ON transactions (user_id);
CREATE INDEX idx_transactions_category_id      ON transactions (category_id);
CREATE INDEX idx_transactions_transaction_date ON transactions (transaction_date);
