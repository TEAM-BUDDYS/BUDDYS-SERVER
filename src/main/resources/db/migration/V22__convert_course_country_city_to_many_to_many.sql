DROP INDEX idx_course_country_created_at ON course;

ALTER TABLE course
    DROP FOREIGN KEY fk_course_country,
    DROP FOREIGN KEY fk_course_city,
    DROP COLUMN country_id,
    DROP COLUMN city_id;

CREATE TABLE course_country
(
    course_id  BIGINT NOT NULL,
    country_id BIGINT NOT NULL,
    PRIMARY KEY (course_id, country_id),
    CONSTRAINT fk_course_country_course  FOREIGN KEY (course_id)  REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_country_country FOREIGN KEY (country_id) REFERENCES country (id)
);
CREATE INDEX idx_course_country_country ON course_country (country_id);

CREATE TABLE course_city
(
    course_id BIGINT NOT NULL,
    city_id   BIGINT NOT NULL,
    PRIMARY KEY (course_id, city_id),
    CONSTRAINT fk_course_city_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_city_city   FOREIGN KEY (city_id)   REFERENCES city (id)
);
CREATE INDEX idx_course_city_city ON course_city (city_id);
