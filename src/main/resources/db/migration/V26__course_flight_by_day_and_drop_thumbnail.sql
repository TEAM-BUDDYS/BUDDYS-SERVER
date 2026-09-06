DELETE FROM course_flight;

ALTER TABLE course_flight
    DROP FOREIGN KEY fk_course_flight_course,
    DROP COLUMN course_id,
    ADD COLUMN course_day_id BIGINT NOT NULL AFTER id;

ALTER TABLE course_flight
    ADD CONSTRAINT fk_course_flight_day FOREIGN KEY (course_day_id) REFERENCES course_day (id) ON DELETE CASCADE;

CREATE INDEX idx_course_flight_day_order ON course_flight (course_day_id, order_no);

ALTER TABLE course
    DROP COLUMN thumbnail_image_url;
