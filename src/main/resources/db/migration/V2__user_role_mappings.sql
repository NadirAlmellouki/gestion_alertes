CREATE TABLE user_role_mappings (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(255) NOT NULL UNIQUE,
    role        VARCHAR(50)  NOT NULL
);
