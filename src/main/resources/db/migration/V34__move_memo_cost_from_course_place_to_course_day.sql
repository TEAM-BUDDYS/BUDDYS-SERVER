ALTER TABLE course_day
    ADD COLUMN memo VARCHAR(500) NULL,
    ADD COLUMN cost DECIMAL(12, 2) NULL;

ALTER TABLE course_place
    DROP COLUMN memo,
    DROP COLUMN cost;
