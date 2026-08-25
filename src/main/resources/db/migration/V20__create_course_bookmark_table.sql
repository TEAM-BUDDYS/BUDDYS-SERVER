CREATE TABLE course_bookmark
(
    user_id    BIGINT      NOT NULL,
    course_id  BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, course_id),
    CONSTRAINT fk_course_bookmark_user   FOREIGN KEY (user_id)   REFERENCES `user` (id)  ON DELETE CASCADE,
    CONSTRAINT fk_course_bookmark_course FOREIGN KEY (course_id) REFERENCES course (id)  ON DELETE CASCADE
);
CREATE INDEX idx_course_bookmark_user_created ON course_bookmark (user_id, created_at);
