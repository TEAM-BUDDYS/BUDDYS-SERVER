CREATE TABLE post_bookmark
(
    user_id    BIGINT      NOT NULL,
    post_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, post_id),
    CONSTRAINT fk_post_bookmark_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_bookmark_post FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
);
