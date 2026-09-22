CREATE TABLE chat_user_block
(
    blocker_id BIGINT      NOT NULL,
    blocked_id BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (blocker_id, blocked_id),
    FOREIGN KEY (blocker_id) REFERENCES `user` (id),
    FOREIGN KEY (blocked_id) REFERENCES `user` (id)
);

CREATE TABLE chat_user_report
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_room_id BIGINT       NOT NULL,
    reporter_id  BIGINT       NOT NULL,
    reported_id  BIGINT       NOT NULL,
    reason       VARCHAR(500) NULL,
    created_at   DATETIME(6)  NOT NULL,
    FOREIGN KEY (chat_room_id) REFERENCES chat_room (id),
    FOREIGN KEY (reporter_id) REFERENCES `user` (id),
    FOREIGN KEY (reported_id) REFERENCES `user` (id)
);
