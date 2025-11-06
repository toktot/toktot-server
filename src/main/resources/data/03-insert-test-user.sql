INSERT INTO users (
    email,
    password,
    auth_provider,
    nickname,
    report_count,
    is_suspended,
    suspension_until,
    warning_count,
    created_at,
    updated_at
) VALUES
      ('test@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '제주도좋아', 0, false, NULL, 0, NOW(), NOW()),
      ('test1@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '한라산등반러버', 0, false, NULL, 0, NOW(), NOW()),
      ('test2@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '오름탐험가', 0, false, NULL, 0, NOW(), NOW()),
      ('test3@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '성산일출봤어요', 0, false, NULL, 0, NOW(), NOW()),
      ('test4@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '흑돼지맛집헌터', 0, false, NULL, 0, NOW(), NOW()),
      ('test5@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '애월카페투어', 0, false, NULL, 0, NOW(), NOW()),
      ('test6@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '서귀포바다러버', 0, false, NULL, 0, NOW(), NOW()),
      ('test7@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '제주감귤농장', 0, false, NULL, 0, NOW(), NOW()),
      ('test8@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '우도땅콩아이스', 0, false, NULL, 0, NOW(), NOW()),
      ('test9@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '섭지코지산책', 0, false, NULL, 0, NOW(), NOW()),
      ('test10@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '제주맛집탐방러', 0, false, NULL, 0, NOW(), NOW());