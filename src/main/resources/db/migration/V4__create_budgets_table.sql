CREATE TABLE budgets (
    id           UUID           NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID           NOT NULL,
    category_id  UUID           NOT NULL,
    amount_limit DECIMAL(19, 2) NOT NULL,
    period       VARCHAR(20)    NOT NULL,
    start_date   DATE           NOT NULL,
    end_date     DATE,
    is_active    BOOLEAN        NOT NULL DEFAULT true,
    created_at   TIMESTAMP      NOT NULL,
    updated_at   TIMESTAMP      NOT NULL,
    CONSTRAINT pk_budgets                PRIMARY KEY (id),
    CONSTRAINT fk_budgets_user           FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category       FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT chk_budgets_period        CHECK (period IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_budgets_end_date      CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_budgets_user_id     ON budgets (user_id);
CREATE INDEX idx_budgets_category_id ON budgets (category_id);
CREATE INDEX idx_budgets_active      ON budgets (is_active);
