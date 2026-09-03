-- 항공편을 코스 전체가 아닌 일자(course_day)에 종속시킨다.
-- 기존 course_flight는 특정 일자와 연결되지 않아 마이그레이션할 대상 일자를 알 수 없으므로 기존 데이터는 비운다.
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
