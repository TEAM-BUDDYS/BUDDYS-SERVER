ALTER TABLE user_tag
    ADD COLUMN display_order INT NULL;

-- 기존 데이터 백필: 사용자별로 카테고리를 번갈아 노출하도록 정렬한다.
-- (카테고리 내 생성순 순번 * 3) + 카테고리 우선순위(ACTIVITY=0, INTEREST=1, TRAVEL_STYLE=2)
-- => display_order 0,1,2 는 각 카테고리의 첫 태그가 되어 기존 대표 태그(카테고리별 1개) 노출과 동일하게 유지된다.
UPDATE user_tag AS target
    JOIN (
        SELECT src.user_id,
               src.tag_id,
               (ROW_NUMBER() OVER (
                   PARTITION BY src.user_id, t.tag_type
                   ORDER BY src.created_at, src.tag_id
               ) - 1) * 3
                   + (FIELD(t.tag_type, 'ACTIVITY', 'INTEREST', 'TRAVEL_STYLE') - 1) AS computed_order
        FROM user_tag AS src
                 JOIN tag AS t ON t.id = src.tag_id
    ) AS ordered
    ON ordered.user_id = target.user_id AND ordered.tag_id = target.tag_id
SET target.display_order = ordered.computed_order;

ALTER TABLE user_tag
    MODIFY COLUMN display_order INT NOT NULL,
    ADD CONSTRAINT uk_user_tag_display_order
    UNIQUE (user_id, display_order);
