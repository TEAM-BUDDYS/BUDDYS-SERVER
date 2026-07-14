INSERT IGNORE INTO post_gender_condition (post_id, gender_condition)
SELECT post_id, 'MALE'
FROM post_gender_condition
WHERE gender_condition = 'ANY';

INSERT IGNORE INTO post_gender_condition (post_id, gender_condition)
SELECT post_id, 'FEMALE'
FROM post_gender_condition
WHERE gender_condition = 'ANY';

DELETE FROM post_gender_condition
WHERE gender_condition = 'ANY';
