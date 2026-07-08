ALTER TABLE post
    ADD COLUMN recruitment_count_type VARCHAR(20)
        AFTER companion_type;

UPDATE post
SET recruitment_count_type = 'UNDECIDED'
WHERE recruitment_count_type IS NULL;

ALTER TABLE post
    MODIFY city_id BIGINT NOT NULL,
    MODIFY start_date DATE NOT NULL,
    MODIFY end_date DATE NOT NULL,
    MODIFY recruitment_count_type VARCHAR(20) NOT NULL,
    DROP COLUMN recruit_count,
    DROP COLUMN min_age,
    DROP COLUMN max_age;

CREATE TABLE post_age_condition
(
    post_id       BIGINT      NOT NULL,
    age_condition VARCHAR(20) NOT NULL,
    PRIMARY KEY (post_id, age_condition),
    FOREIGN KEY (post_id) REFERENCES post (id)
);
