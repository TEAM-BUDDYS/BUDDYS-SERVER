ALTER TABLE course
    ADD COLUMN comment_count BIGINT NOT NULL DEFAULT 0 AFTER view_count;

CREATE TABLE course_comment
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id  BIGINT       NOT NULL,
    author_id  BIGINT       NOT NULL,
    content    VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT fk_course_comment_course FOREIGN KEY (course_id) REFERENCES course (id),
    CONSTRAINT fk_course_comment_author FOREIGN KEY (author_id) REFERENCES `user` (id)
);

CREATE INDEX idx_course_comment_course_id_created_at ON course_comment (course_id, created_at);
