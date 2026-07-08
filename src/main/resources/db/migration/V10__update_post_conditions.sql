ALTER TABLE post
    ADD COLUMN recruitment_count_type VARCHAR(20)
        AFTER companion_type;

UPDATE post
SET recruitment_count_type = 'UNDECIDED'
WHERE recruitment_count_type IS NULL;

UPDATE post p
SET city_id = (
    SELECT MIN(c.id)
    FROM city c
    WHERE c.country_id = p.country_id
)
WHERE city_id IS NULL;

UPDATE post
SET start_date = COALESCE(start_date, DATE(created_at), CURRENT_DATE),
    end_date = COALESCE(end_date, start_date, DATE(created_at), CURRENT_DATE)
WHERE start_date IS NULL
   OR end_date IS NULL;

CREATE TABLE post_age_condition
(
    post_id       BIGINT      NOT NULL,
    age_condition VARCHAR(20) NOT NULL,
    PRIMARY KEY (post_id, age_condition),
    FOREIGN KEY (post_id) REFERENCES post (id)
);

INSERT INTO post_age_condition (post_id, age_condition)
SELECT id, 'EARLY_20S'
FROM post
WHERE (min_age IS NOT NULL OR max_age IS NOT NULL)
  AND (min_age IS NULL OR min_age <= 23)
  AND (max_age IS NULL OR max_age >= 20);

INSERT INTO post_age_condition (post_id, age_condition)
SELECT id, 'MID_20S'
FROM post
WHERE (min_age IS NOT NULL OR max_age IS NOT NULL)
  AND (min_age IS NULL OR min_age <= 26)
  AND (max_age IS NULL OR max_age >= 24);

INSERT INTO post_age_condition (post_id, age_condition)
SELECT id, 'LATE_20S'
FROM post
WHERE (min_age IS NOT NULL OR max_age IS NOT NULL)
  AND (min_age IS NULL OR min_age <= 29)
  AND (max_age IS NULL OR max_age >= 27);

INSERT INTO post_age_condition (post_id, age_condition)
SELECT id, 'OVER_30S'
FROM post
WHERE (min_age IS NOT NULL OR max_age IS NOT NULL)
  AND (max_age IS NULL OR max_age >= 30);

ALTER TABLE post
    MODIFY city_id BIGINT NOT NULL,
    MODIFY start_date DATE NOT NULL,
    MODIFY end_date DATE NOT NULL,
    MODIFY recruitment_count_type VARCHAR(20) NOT NULL,
    DROP COLUMN recruit_count,
    DROP COLUMN min_age,
    DROP COLUMN max_age;
