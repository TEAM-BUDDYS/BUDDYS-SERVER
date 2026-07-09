CREATE TABLE post_comment
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT        NOT NULL,
    author_id  BIGINT        NOT NULL,
    content    VARCHAR(100)  NOT NULL,
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL,
    FOREIGN KEY (post_id) REFERENCES post (id),
    FOREIGN KEY (author_id) REFERENCES `user` (id)
);

CREATE INDEX idx_post_comment_post_id_created_at ON post_comment (post_id, created_at);
