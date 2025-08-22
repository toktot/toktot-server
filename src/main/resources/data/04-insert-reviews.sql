-- =============================================
-- 델문도 리뷰 데이터 INSERT SQL
-- 기준: JSON 데이터를 기반으로 한 단일 리뷰 생성
-- =============================================

-- 1. 매장 정보 먼저 확인/삽입 (external_kakao_id 기준)
-- restaurants 테이블에 델문도 매장이 없다면 먼저 삽입해야 함
INSERT INTO restaurants (
    external_kakao_id,
    name,
    latitude,
    longitude,
    address,
    category,
    data_source,
    is_active,
    created_at,
    is_good_price_store,
    is_local_store,
                         search_count,
    last_synced_at
) VALUES (
             '26867476',
             '델문도',
             33.5437093358268,
             126.668723957376,
             '제주특별자치도 제주시 조천읍 함덕리 1008-1', -- 실제 주소로 수정 필요
             '음식점 > 카페',
             'KAKAO',
             true,
             '2024-08-22 18:00:00',
          false,
            false,
          0,
             '2024-08-22 18:00:00'
         );
-- 2. 리뷰 데이터 삽입
-- restaurant_id는 매장의 external_kakao_id로 찾아서 사용
INSERT INTO reviews (
    user_id,
    restaurant_id,
    meal_time,
    value_for_money_score,
    report_count,
    is_hidden,
    created_at,
    updated_at
) VALUES (
             1, -- user_id
             (SELECT id FROM restaurants WHERE external_kakao_id = '26867476'), -- restaurant_id를 동적으로 찾기
             'DINNER', -- meal_time
             100, -- value_for_money_score
             0, -- report_count
             false, -- is_hidden
             '2024-08-22 18:00:00',
             '2024-08-22 18:00:00'
         );

-- 3. 리뷰 이미지 데이터 삽입
-- review_id는 방금 생성된 리뷰의 ID를 동적으로 찾기
INSERT INTO review_images (
    review_id,
    image_id,
    s3_key,
    image_url,
    file_size,
    image_order,
    is_main,
    created_at
) VALUES
-- 첫 번째 이미지 (대표 이미지)
(
    (SELECT MAX(id) FROM reviews WHERE user_id = 1 AND restaurant_id = (SELECT id FROM restaurants WHERE external_kakao_id = '26867476')), -- 방금 생성된 review_id
    '92af0f64-66bd-45c6-b468-383bce378ca2', -- image_id
    '92af0f64-66bd-45c6-b468-383bce378ca2.jpg', -- s3_key
    'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/92af0f64-66bd-45c6-b468-383bce378ca2.jpg', -- image_url
    2048576, -- file_size (2MB)
    1, -- image_order
    true, -- is_main (대표 이미지)
    '2024-08-22 18:00:00'
),
-- 두 번째 이미지
(
    (SELECT MAX(id) FROM reviews WHERE user_id = 1 AND restaurant_id = (SELECT id FROM restaurants WHERE external_kakao_id = '26867476')), -- 방금 생성된 review_id
    '34a1f959-85ff-48c9-8e3d-0b0ae6abdc83', -- image_id
    '34a1f959-85ff-48c9-8e3d-0b0ae6abdc83.jpg', -- s3_key
    'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/34a1f959-85ff-48c9-8e3d-0b0ae6abdc83.jpg', -- image_url
    1876543, -- file_size (약 1.8MB)
    2, -- image_order
    false, -- is_main
    '2024-08-22 18:00:00'
);

-- 4. 리뷰 키워드 데이터 삽입
INSERT INTO review_keywords (
    review_id,
    keyword_type,
    created_at
) VALUES
      ((SELECT MAX(id) FROM reviews WHERE user_id = 1 AND restaurant_id = (SELECT id FROM restaurants WHERE external_kakao_id = '26867476')), 'QUIET_ATMOSPHERE', '2024-08-22 18:00:00'),
      ((SELECT MAX(id) FROM reviews WHERE user_id = 1 AND restaurant_id = (SELECT id FROM restaurants WHERE external_kakao_id = '26867476')), 'CLEAN_STORE', '2024-08-22 18:00:00'),
      ((SELECT MAX(id) FROM reviews WHERE user_id = 1 AND restaurant_id = (SELECT id FROM restaurants WHERE external_kakao_id = '26867476')), 'RESERVATION_REQUIRED', '2024-08-22 18:00:00'),
      ((SELECT MAX(id) FROM reviews WHERE user_id = 1 AND restaurant_id = (SELECT id FROM restaurants WHERE external_kakao_id = '26867476')), 'DELICIOUS', '2024-08-22 18:00:00');

-- 5. 툴팁 데이터 삽입
INSERT INTO tooltips (
    review_image_id,
    tooltip_type,
    x_position,
    y_position,
    rating,
    menu_name,
    total_price,
    serving_size,
    detailed_review,
    created_at
) VALUES
-- 첫 번째 이미지의 툴팁들 (image_order = 1인 이미지)
(
    (SELECT id FROM review_images WHERE image_id = '92af0f64-66bd-45c6-b468-383bce378ca2'), -- 첫 번째 이미지 ID
    'FOOD', -- tooltip_type
    45.5, -- x_position
    67.2, -- y_position
    4.5, -- rating
    '제주 갈치조림', -- menu_name
    25000, -- total_price
    2, -- serving_size
    '신선하고 맛있어요', -- detailed_review
    '2024-08-22 18:00:00'
),
(
    (SELECT id FROM review_images WHERE image_id = '92af0f64-66bd-45c6-b468-383bce378ca2'), -- 첫 번째 이미지 ID
    'SERVICE', -- tooltip_type
    20.0, -- x_position
    30.5, -- y_position
    4.0, -- rating
    NULL, -- menu_name (서비스 툴팁이므로 NULL)
    NULL, -- total_price (서비스 툴팁이므로 NULL)
    NULL, -- serving_size (서비스 툴팁이므로 NULL)
    '서비스 상세 리뷰 본문이다.', -- detailed_review
    '2024-08-22 18:00:00'
),
-- 두 번째 이미지의 툴팁들 (image_order = 2인 이미지)
(
    (SELECT id FROM review_images WHERE image_id = '34a1f959-85ff-48c9-8e3d-0b0ae6abdc83'), -- 두 번째 이미지 ID
    'FOOD', -- tooltip_type
    45.5, -- x_position
    67.2, -- y_position
    4.5, -- rating
    '성게 미역국', -- menu_name
    25000, -- total_price
    2, -- serving_size
    '신선하고 맛있어요', -- detailed_review
    '2024-08-22 18:00:00'
),
(
    (SELECT id FROM review_images WHERE image_id = '34a1f959-85ff-48c9-8e3d-0b0ae6abdc83'), -- 두 번째 이미지 ID
    'SERVICE', -- tooltip_type
    20.0, -- x_position
    30.5, -- y_position
    4.0, -- rating
    NULL, -- menu_name (서비스 툴팁이므로 NULL)
    NULL, -- total_price (서비스 툴팁이므로 NULL)
    NULL, -- serving_size (서비스 툴팁이므로 NULL)
    '서비스 상세 리뷰 본문이다.', -- detailed_review
    '2024-08-22 18:00:00'
);