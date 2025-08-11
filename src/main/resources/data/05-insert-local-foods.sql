-- 1. 기본 데이터 삽입 (활성화된 향토음식들)
INSERT INTO local_foods (local_food_type, display_order, is_active, created_at, updated_at) VALUES
-- 제주 대표 향토음식 (메인 노출용)
('DOMBE_MEAT', 1, true, NOW(), NOW()),
('MEAT_NOODLE_SOUP', 2, true, NOW(), NOW()),
('SEA_URCHIN_SEAWEED_SOUP', 3, true, NOW(), NOW()),
('BRACKEN_HANGOVER_SOUP', 4, true, NOW(), NOW()),
('GRILLED_RED_TILEFISH', 5, true, NOW(), NOW()),
('GRILLED_CUTLASSFISH', 6, true, NOW(), NOW()),
('RAW_FISH_MULHOE', 7, true, NOW(), NOW()),

-- 전통 떡류 (서브 카테고리)
('BING_RICE_CAKE', 8, true, NOW(), NOW()),
('OMEGI_RICE_CAKE', 9, true, NOW(), NOW());