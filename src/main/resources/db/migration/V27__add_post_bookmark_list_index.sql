CREATE INDEX idx_post_bookmark_user_created_post
    ON post_bookmark (user_id, created_at DESC, post_id DESC);
