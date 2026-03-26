CREATE TABLE savings_goals (
    id             UUID           NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID           NOT NULL,
    name           VARCHAR(100)   NOT NULL,
    description    VARCHAR(500),
    target_amount  DECIMAL(19, 2) NOT NULL,
    current_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    target_date    DATE,
    is_completed   BOOLEAN        NOT NULL DEFAULT false,
    created_at     TIMESTAMP      NOT NULL,
    updated_at     TIMESTAMP      NOT NULL,
    CONSTRAINT pk_savings_goals               PRIMARY KEY (id),
    CONSTRAINT fk_savings_goals_user          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_savings_goals_target       CHECK (target_amount > 0),
    CONSTRAINT chk_savings_goals_current      CHECK (current_amount >= 0)
);

CREATE INDEX idx_savings_goals_user_id   ON savings_goals (user_id);
CREATE INDEX idx_savings_goals_completed ON savings_goals (is_completed);
