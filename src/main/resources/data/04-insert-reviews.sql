-- 더미 리뷰 데이터 (reviews 테이블)
INSERT INTO reviews (user_id, restaurant_id, created_at, updated_at) VALUES
-- 제주 흑돼지 전문점 리뷰들 (실제 존재하는 user_id, restaurant_id로 수정 필요)
(1, 1, '2024-08-01 12:30:00', '2024-08-01 12:30:00'),
(1, 1, '2024-08-02 18:45:00', '2024-08-02 18:45:00'),
(1, 1, '2024-08-03 20:15:00', '2024-08-03 20:15:00'),

-- 제주 갈치구이 맛집 리뷰들
(1, 2, '2024-08-01 13:20:00', '2024-08-01 13:20:00'),
(1, 2, '2024-08-04 19:30:00', '2024-08-04 19:30:00'),
(1, 2, '2024-08-05 14:45:00', '2024-08-05 14:45:00'),

-- 성산 해녀의집 리뷰들
(1, 3, '2024-08-02 11:15:00', '2024-08-02 11:15:00'),
(1, 3, '2024-08-06 16:20:00', '2024-08-06 16:20:00'),

-- 서귀포 몸국 전문점 리뷰들
(1, 4, '2024-08-03 12:45:00', '2024-08-03 12:45:00'),
(1, 4, '2024-08-07 13:30:00', '2024-08-07 13:30:00'),

-- 제주 전복죽 맛집 리뷰들
(1, 5, '2024-08-04 15:20:00', '2024-08-04 15:20:00'),
(1, 5, '2024-08-08 11:45:00', '2024-08-08 11:45:00'),

-- 한림 고등어구이 리뷰들
(1, 6, '2024-08-05 18:10:00', '2024-08-05 18:10:00'),
(1, 6, '2024-08-09 19:25:00', '2024-08-09 19:25:00'),

-- 애월 돼지국밥 리뷰들
(1, 7, '2024-08-06 08:30:00', '2024-08-06 08:30:00'),
(1, 7, '2024-08-10 09:15:00', '2024-08-10 09:15:00');

-- 리뷰 이미지 데이터 (review_images 테이블)
INSERT INTO review_images (review_id, image_id, s3_key, image_url, file_size, image_order, created_at) VALUES
-- 리뷰 1의 이미지들 (흑돼지 전문점)
(1, 'img_001_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2048576, 1, '2024-08-01 12:30:00'),
(1, 'img_001_2', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 1825739, 2, '2024-08-01 12:30:00'),

-- 리뷰 2의 이미지들
(2, 'img_002_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 3145728, 1, '2024-08-02 18:45:00'),
(2, 'img_002_2', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2234567, 2, '2024-08-02 18:45:00'),

-- 리뷰 3의 이미지들
(3, 'img_003_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2567890, 1, '2024-08-03 20:15:00'),

-- 리뷰 4의 이미지들 (갈치구이)
(4, 'img_004_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2890123, 1, '2024-08-01 13:20:00'),
(4, 'img_004_2', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2345678, 2, '2024-08-01 13:20:00'),

-- 리뷰 5의 이미지들
(5, 'img_005_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 3456789, 1, '2024-08-04 19:30:00'),
(5, 'img_005_2', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2123456, 2, '2024-08-04 19:30:00'),

-- 리뷰 6의 이미지들
(6, 'img_006_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2678901, 1, '2024-08-05 14:45:00'),

-- 리뷰 7의 이미지들 (성산 해녀의집)
(7, 'img_007_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2987654, 1, '2024-08-02 11:15:00'),
(7, 'img_007_2', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2456789, 2, '2024-08-02 11:15:00'),

-- 리뷰 8의 이미지들
(8, 'img_008_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 3234567, 1, '2024-08-06 16:20:00'),

-- 나머지 리뷰들도 각각 1-2개씩 이미지 추가
(9, 'img_009_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2765432, 1, '2024-08-03 12:45:00'),
(10, 'img_010_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2543210, 1, '2024-08-07 13:30:00'),
(11, 'img_011_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2876543, 1, '2024-08-04 15:20:00'),
(12, 'img_012_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2654321, 1, '2024-08-08 11:45:00'),
(13, 'img_013_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2987123, 1, '2024-08-05 18:10:00'),
(14, 'img_014_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2345987, 1, '2024-08-09 19:25:00'),
(15, 'img_015_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2198765, 1, '2024-08-06 08:30:00'),
(16, 'img_016_1', 'Property+1%3DVariant3.png', 'https://toktot-dev-images.s3.ap-northeast-2.amazonaws.com/Property+1%3DVariant3.png', 2567123, 1, '2024-08-10 09:15:00');

-- 리뷰 키워드 데이터 (review_keywords 테이블)
INSERT INTO review_keywords (review_id, keyword_type, created_at) VALUES
-- 흑돼지 전문점 리뷰들의 키워드
(1, 'LOCAL', '2024-08-01 12:30:00'),
(1, 'COZY', '2024-08-01 12:30:00'),
(2, 'LOCAL_POPULAR', '2024-08-02 18:45:00'),
(2, 'TRENDY', '2024-08-02 18:45:00'),
(3, 'QUIET', '2024-08-03 20:15:00'),

-- 갈치구이 맛집 리뷰들의 키워드
(4, 'OCEAN_VIEW', '2024-08-01 13:20:00'),
(4, 'LOCAL', '2024-08-01 13:20:00'),
(5, 'LOCAL_POPULAR', '2024-08-04 19:30:00'),
(5, 'COZY', '2024-08-04 19:30:00'),
(6, 'TRENDY', '2024-08-05 14:45:00'),

-- 성산 해녀의집 리뷰들의 키워드
(7, 'OCEAN_VIEW', '2024-08-02 11:15:00'),
(7, 'LOCAL', '2024-08-02 11:15:00'),
(8, 'MOUNTAIN_VIEW', '2024-08-06 16:20:00'),
(8, 'QUIET', '2024-08-06 16:20:00'),

-- 몸국 전문점 리뷰들의 키워드
(9, 'LOCAL_POPULAR', '2024-08-03 12:45:00'),
(10, 'COZY', '2024-08-07 13:30:00'),

-- 전복죽 맛집 리뷰들의 키워드
(11, 'QUIET', '2024-08-04 15:20:00'),
(11, 'LOCAL', '2024-08-04 15:20:00'),
(12, 'COZY', '2024-08-08 11:45:00'),

-- 고등어구이 리뷰들의 키워드
(13, 'OCEAN_VIEW', '2024-08-05 18:10:00'),
(13, 'LOCAL_POPULAR', '2024-08-05 18:10:00'),
(14, 'TRENDY', '2024-08-09 19:25:00'),

-- 돼지국밥 리뷰들의 키워드
(15, 'LOCAL', '2024-08-06 08:30:00'),
(16, 'COZY', '2024-08-10 09:15:00');

-- 툴팁 데이터 (tooltips 테이블)
INSERT INTO tooltips (review_image_id, tooltip_type, x_position, y_position, rating, menu_name, total_price, serving_size, detailed_review, created_at) VALUES
-- 리뷰 1의 이미지들에 대한 툴팁들 (흑돼지 전문점)
(1, 'FOOD', 45.50, 60.25, 4.5, '흑돼지 구이', 35000, 2, '고기가 정말 부드럽고 맛있어요!', '2024-08-01 12:30:00'),
(1, 'FOOD', 70.20, 40.80, 4.0, '된장찌개', 8000, 2, '구수하고 깔끔한 맛', '2024-08-01 12:30:00'),
(2, 'SERVICE', 30.75, 50.30, 4.2, NULL, NULL, NULL, NULL, '2024-08-01 12:30:00'),

-- 리뷰 2의 이미지들에 대한 툴팁들
(3, 'FOOD', 55.40, 45.60, 4.8, '제주 흑돼지 세트', 42000, 3, '3인분인데 양도 푸짐하고 맛도 최고!', '2024-08-02 18:45:00'),
(4, 'FOOD', 40.30, 35.70, 4.3, '볶음밥', 8000, 1, '고기 기름으로 볶아서 고소해요', '2024-08-02 18:45:00'),

-- 리뷰 3의 이미지들에 대한 툴팁들
(5, 'FOOD', 50.90, 42.15, 4.1, '오겹살', 28000, 2, '두께가 적당하고 육즙이 좋아요', '2024-08-03 20:15:00'),

-- 리뷰 4의 이미지들에 대한 툴팁들 (갈치구이)
(6, 'FOOD', 48.60, 52.30, 4.6, '갈치구이 정식', 18000, 1, '갈치가 신선하고 양념이 일품!', '2024-08-01 13:20:00'),
(7, 'FOOD', 65.20, 38.45, 4.4, '갈치조림', 16000, 1, '매콤달콤한 맛이 좋아요', '2024-08-01 13:20:00'),

-- 리뷰 5의 이미지들에 대한 툴팁들
(8, 'FOOD', 42.80, 48.90, 4.7, '갈치회', 25000, 2, '회가 쫄깃하고 신선해요', '2024-08-04 19:30:00'),
(9, 'SERVICE', 35.50, 60.25, 4.5, NULL, NULL, NULL, NULL, '2024-08-04 19:30:00'),

-- 리뷰 6의 이미지들에 대한 툴팁들
(10, 'FOOD', 52.40, 46.80, 4.3, '갈치구이 세트', 22000, 1, '반찬도 맛있고 구성이 좋아요', '2024-08-05 14:45:00'),

-- 리뷰 7의 이미지들에 대한 툴팁들 (성산 해녀의집)
(11, 'FOOD', 47.30, 51.60, 4.8, '성게국', 15000, 1, '성게가 가득하고 진짜 맛있어요!', '2024-08-02 11:15:00'),
(12, 'FOOD', 62.90, 41.20, 4.4, '전복구이', 20000, 1, '전복이 통통하고 쫄깃해요', '2024-08-02 11:15:00'),

-- 리뷰 8의 이미지들에 대한 툴팁들
(13, 'FOOD', 44.70, 49.30, 4.6, '해녀물회', 18000, 1, '시원하고 해산물이 신선해요', '2024-08-06 16:20:00'),

-- 리뷰 9의 이미지들에 대한 툴팁들 (몸국)
(14, 'FOOD', 51.20, 47.80, 4.5, '몸국', 12000, 1, '진짜 몸보신되는 느낌!', '2024-08-03 12:45:00'),

-- 리뷰 10의 이미지들에 대한 툴팁들
(15, 'FOOD', 46.40, 53.10, 4.3, '몸국 세트', 15000, 1, '반찬도 푸짐하고 좋아요', '2024-08-07 13:30:00'),

-- 리뷰 11의 이미지들에 대한 툴팁들 (전복죽)
(16, 'FOOD', 49.80, 45.70, 4.7, '전복죽', 14000, 1, '진짜 부드럽고 고소해요', '2024-08-04 15:20:00'),

-- 리뷰 12의 이미지들에 대한 툴팁들
(17, 'FOOD', 53.60, 50.40, 4.4, '전복죽 특', 18000, 1, '전복이 크고 맛있어요', '2024-08-08 11:45:00'),

-- 리뷰 13의 이미지들에 대한 툴팁들 (고등어구이)
(18, 'FOOD', 41.90, 48.20, 4.6, '고등어구이', 13000, 1, '기름이 적당하고 맛있어요', '2024-08-05 18:10:00'),

-- 리뷰 14의 이미지들에 대한 툴팁들
(19, 'FOOD', 57.30, 43.90, 4.2, '고등어조림', 12000, 1, '매콤한 양념이 좋아요', '2024-08-09 19:25:00'),

-- 리뷰 15의 이미지들에 대한 툴팁들 (돼지국밥)
(20, 'FOOD', 45.10, 52.60, 4.5, '돼지국밥', 8000, 1, '진짜 진한 국물맛!', '2024-08-06 08:30:00'),

-- 리뷰 16의 이미지들에 대한 툴팁들
(21, 'FOOD', 50.70, 46.40, 4.3, '순대국밥', 8500, 1, '순대도 맛있고 국물도 좋아요', '2024-08-10 09:15:00');