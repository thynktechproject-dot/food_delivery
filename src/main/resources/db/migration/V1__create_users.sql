CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(100),
                       email VARCHAR(100) UNIQUE,
                       password VARCHAR(255),
                       role VARCHAR(20),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);