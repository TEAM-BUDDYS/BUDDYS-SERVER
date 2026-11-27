CREATE TABLE post_companion_type
(
    post_id        BIGINT      NOT NULL,
    companion_type VARCHAR(30) NOT NULL,
    PRIMARY KEY (post_id, companion_type),
    FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
);

INSERT INTO post_companion_type (post_id, companion_type)
SELECT id, companion_type
FROM post;

ALTER TABLE post DROP COLUMN companion_type;
