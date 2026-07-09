ALTER TABLE post
    ADD COLUMN recruitment_count_type VARCHAR(20)
        AFTER companion_type;

CREATE TABLE post_age_condition
(
    post_id       BIGINT      NOT NULL,
    age_condition VARCHAR(20) NOT NULL,
    PRIMARY KEY (post_id, age_condition),
    FOREIGN KEY (post_id) REFERENCES post (id)
);

CREATE TABLE post_gender_condition
(
    post_id          BIGINT      NOT NULL,
    gender_condition VARCHAR(10) NOT NULL,
    PRIMARY KEY (post_id, gender_condition),
    FOREIGN KEY (post_id) REFERENCES post (id)
);

ALTER TABLE post
    MODIFY city_id BIGINT NOT NULL,
    MODIFY start_date DATE NOT NULL,
    MODIFY end_date DATE NOT NULL,
    MODIFY recruitment_count_type VARCHAR(20) NOT NULL,
    DROP COLUMN recruit_count,
    DROP COLUMN min_age,
    DROP COLUMN max_age,
    DROP COLUMN gender_condition;
