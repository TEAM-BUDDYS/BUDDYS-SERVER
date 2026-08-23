CREATE TABLE course
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_id  BIGINT       NOT NULL,
    country_id BIGINT       NOT NULL,
    city_id    BIGINT       NOT NULL,
    title      VARCHAR(120) NOT NULL,
    content    TEXT,
    start_date DATE         NOT NULL,
    end_date   DATE         NOT NULL,
    view_count BIGINT       NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    deleted_at DATETIME(6),
    CONSTRAINT fk_course_author  FOREIGN KEY (author_id)  REFERENCES `user` (id),
    CONSTRAINT fk_course_country FOREIGN KEY (country_id) REFERENCES country (id),
    CONSTRAINT fk_course_city    FOREIGN KEY (city_id)    REFERENCES city (id)
);
CREATE INDEX idx_course_country_created_at ON course (country_id, created_at);

CREATE TABLE course_tag
(
    course_id BIGINT NOT NULL,
    tag_id    BIGINT NOT NULL,
    PRIMARY KEY (course_id, tag_id),
    CONSTRAINT fk_course_tag_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_tag_tag    FOREIGN KEY (tag_id)    REFERENCES tag (id)
);

CREATE TABLE course_day
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id  BIGINT   NOT NULL,
    day_number SMALLINT NOT NULL,   -- 1부터 시작
    date       DATE,
    CONSTRAINT uk_course_day UNIQUE (course_id, day_number),
    CONSTRAINT fk_course_day_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE
);

CREATE TABLE course_image
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_day_id BIGINT       NOT NULL,
    image_url     VARCHAR(512) NOT NULL,
    order_no      SMALLINT     NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT fk_course_image_day FOREIGN KEY (course_day_id) REFERENCES course_day (id) ON DELETE CASCADE
);
CREATE INDEX idx_course_image_day_order ON course_image (course_day_id, order_no);

CREATE TABLE course_place
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_day_id BIGINT        NOT NULL,
    place_id      BIGINT        NOT NULL,
    order_no      SMALLINT      NOT NULL DEFAULT 0,
    memo          VARCHAR(500),
    cost          DECIMAL(12, 2),
    created_at    DATETIME(6)   NOT NULL,
    CONSTRAINT fk_course_place_day   FOREIGN KEY (course_day_id) REFERENCES course_day (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_place_place FOREIGN KEY (place_id)      REFERENCES place (id)
);
CREATE INDEX idx_course_place_day_order ON course_place (course_day_id, order_no);

CREATE TABLE course_companion
(
    course_id  BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (course_id, user_id),
    CONSTRAINT fk_course_companion_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_companion_user   FOREIGN KEY (user_id)   REFERENCES `user` (id)
);

CREATE TABLE course_flight
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id         BIGINT       NOT NULL,
    airline           VARCHAR(100) NOT NULL,   -- 항공사
    flight_number     VARCHAR(20),             -- 항공편명
    departure_airport VARCHAR(100) NOT NULL,   -- 출발 공항
    departure_at      DATETIME(6)  NOT NULL,   -- 출발일 + 출발 시간
    arrival_airport   VARCHAR(100) NOT NULL,   -- 도착 공항
    arrival_at        DATETIME(6)  NOT NULL,   -- 도착일 + 도착 시간
    order_no          SMALLINT     NOT NULL DEFAULT 0,
    created_at        DATETIME(6)  NOT NULL,
    CONSTRAINT fk_course_flight_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE
);
