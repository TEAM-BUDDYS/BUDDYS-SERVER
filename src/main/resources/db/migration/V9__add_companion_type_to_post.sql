ALTER TABLE post
    ADD COLUMN companion_type VARCHAR(30)
        AFTER max_age;

UPDATE post
SET companion_type = 'FULL_TRIP'
WHERE companion_type IS NULL;

ALTER TABLE post
    MODIFY companion_type VARCHAR(30) NOT NULL;
