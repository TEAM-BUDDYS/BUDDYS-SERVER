ALTER TABLE chat_room
    ADD COLUMN direct_chat_key VARCHAR(50);

ALTER TABLE chat_room
    ADD CONSTRAINT uk_chat_room_direct_chat_key UNIQUE (direct_chat_key);
