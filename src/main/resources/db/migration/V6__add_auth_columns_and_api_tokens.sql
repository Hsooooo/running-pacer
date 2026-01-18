-- V6__add_auth_columns_and_api_tokens.sql

-- users 테이블 확장
ALTER TABLE users ADD COLUMN email VARCHAR(255) UNIQUE;
ALTER TABLE users ADD COLUMN provider VARCHAR(20); -- GOOGLE, APPLE
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);

-- api_tokens 테이블 생성
CREATE TABLE api_tokens (
    token_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE INDEX idx_api_tokens_token ON api_tokens(token);
