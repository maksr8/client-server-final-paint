CREATE TABLE shared_drawings (
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    drawing_id UUID NOT NULL REFERENCES drawings(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, drawing_id)
);
