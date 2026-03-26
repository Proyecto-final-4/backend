CREATE TABLE categories (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    color      VARCHAR(7),
    icon       VARCHAR(50),
    is_system  BOOLEAN      NOT NULL DEFAULT false,
    user_id    UUID,
    parent_id  UUID,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT fk_categories_user   FOREIGN KEY (user_id)   REFERENCES users(id)       ON DELETE SET NULL,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id)  ON DELETE SET NULL
);
