ALTER TABLE magazine
    ADD COLUMN category VARCHAR(30) NULL;

CREATE INDEX idx_magazine_category_published_at
    ON magazine (category, published_at DESC, id DESC);
