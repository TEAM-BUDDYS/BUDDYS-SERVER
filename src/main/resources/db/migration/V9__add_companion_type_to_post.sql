ALTER TABLE post
    ADD COLUMN companion_type VARCHAR(30) NOT NULL DEFAULT 'FULL_TRIP'
        AFTER max_age;
