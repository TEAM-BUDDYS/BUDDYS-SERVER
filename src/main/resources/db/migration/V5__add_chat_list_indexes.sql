CREATE INDEX idx_chat_room_member_user_room
    ON chat_room_member (user_id, chat_room_id);

CREATE INDEX idx_chat_message_room_created_at
    ON chat_message (chat_room_id, created_at);
