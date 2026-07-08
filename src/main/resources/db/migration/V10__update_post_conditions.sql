ALTER TABLE post
    ADD COLUMN recruitment_count_type VARCHAR(20) NOT NULL DEFAULT 'UNDECIDED'
        AFTER companion_type,
    DROP COLUMN recruit_count,
    DROP COLUMN min_age,
    DROP COLUMN max_age;

ALTER TABLE post
    ALTER companion_type DROP DEFAULT,
    ALTER recruitment_count_type DROP DEFAULT;

CREATE TABLE post_age_condition
(
    post_id       BIGINT      NOT NULL,
    age_condition VARCHAR(20) NOT NULL,
    PRIMARY KEY (post_id, age_condition),
    FOREIGN KEY (post_id) REFERENCES post (id)
);
