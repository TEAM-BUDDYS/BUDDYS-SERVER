CREATE INDEX idx_chat_room_member_user_room
    ON chat_room_member (user_id, chat_room_id);

CREATE INDEX idx_chat_message_room_created_at
    ON chat_message (chat_room_id, created_at, id);

ALTER TABLE chat_room_member
    ADD COLUMN last_read_message_id BIGINT;

ALTER TABLE chat_message
    ADD CONSTRAINT uk_chat_message_room_id UNIQUE (chat_room_id, id);

ALTER TABLE chat_room_member
    ADD CONSTRAINT fk_chat_room_member_last_read_message
        FOREIGN KEY (chat_room_id, last_read_message_id)
            REFERENCES chat_message (chat_room_id, id);

ALTER TABLE chat_room_member
    DROP COLUMN last_read_at;
