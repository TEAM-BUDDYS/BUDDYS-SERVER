CREATE INDEX idx_post_start_date_status_deleted_at_created_at
    ON post (start_date, status, deleted_at, created_at);
