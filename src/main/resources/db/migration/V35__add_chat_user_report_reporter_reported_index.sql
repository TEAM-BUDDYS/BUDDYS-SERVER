CREATE INDEX idx_chat_user_report_reporter_reported_created_at
    ON chat_user_report (reporter_id, reported_id, created_at);
