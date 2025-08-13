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
      ('test@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '테스트사용자1', 0, false, NULL, 0, NOW(), NOW()),
      ('test1@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '테스트사용자1', 0, false, NULL, 0, NOW(), NOW()),
      ('test2@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '테스트사용자2', 0, false, NULL, 0, NOW(), NOW()),
      ('test3@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '테스트사용자3', 0, false, NULL, 0, NOW(), NOW()),
      ('test4@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '테스트사용자4', 0, false, NULL, 0, NOW(), NOW()),
      ('test5@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '테스트사용자5', 0, false, NULL, 0, NOW(), NOW()),
      ('test6@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '테스트사용자6', 0, false, NULL, 0, NOW(), NOW()),
      ('test7@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '테스트사용자7', 0, false, NULL, 0, NOW(), NOW()),
      ('test8@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '테스트사용자8', 0, false, NULL, 0, NOW(), NOW()),
      ('test9@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '테스트사용자9', 0, false, NULL, 0, NOW(), NOW()),
      ('test10@toktot.com', '$2a$10$qY6Vt7tYeD6jqkIp5Tv8mO/Ps6tkdhGDa20mw0v3pFv7OIBx6ZGsq', 'EMAIL', '테스트사용자10', 0, false, NULL, 0, NOW(), NOW());