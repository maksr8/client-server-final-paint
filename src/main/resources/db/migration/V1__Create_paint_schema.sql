CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE drawings (
                          id SERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          owner_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          data TEXT,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);