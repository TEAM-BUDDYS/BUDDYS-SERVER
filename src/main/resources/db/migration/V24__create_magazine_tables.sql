CREATE TABLE magazine
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(120) NOT NULL,
    summary             VARCHAR(255) NOT NULL,
    thumbnail_image_url VARCHAR(512) NOT NULL,
    external_url        VARCHAR(512) NOT NULL,
    published_at        DATE         NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL
);

CREATE INDEX idx_magazine_published_at ON magazine (published_at, id);

CREATE TABLE magazine_bookmark
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    magazine_id BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    CONSTRAINT uk_magazine_bookmark_user_magazine UNIQUE (user_id, magazine_id),
    CONSTRAINT fk_magazine_bookmark_user     FOREIGN KEY (user_id)     REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_magazine_bookmark_magazine FOREIGN KEY (magazine_id) REFERENCES magazine (id) ON DELETE CASCADE
);

CREATE INDEX idx_magazine_bookmark_user_created ON magazine_bookmark (user_id, created_at);
