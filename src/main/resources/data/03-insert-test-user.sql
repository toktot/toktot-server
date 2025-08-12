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
      ('test1@toktot.com', '$2a$10$N9qo8uLOickgx2ZMRg.opu.7afdnkmRW8e8Y4qj2j0kxK8J/jH/L6', 'EMAIL', '테스트사용자1', 0, false, NULL, 0, NOW(), NOW()),
      ('test2@toktot.com', '$2a$10$N9qo8uLOickgx2ZMRg.opu.7afdnkmRW8e8Y4qj2j0kxK8J/jH/L6', 'EMAIL', '테스트사용자2', 0, false, NULL, 0, NOW(), NOW()),
      ('test3@toktot.com', '$2a$10$N9qo8uLOickgx2ZMRg.opu.7afdnkmRW8e8Y4qj2j0kxK8J/jH/L6', 'EMAIL', '테스트사용자3', 0, false, NULL, 0, NOW(), NOW()),
      ('test4@toktot.com', '$2a$10$N9qo8uLOickgx2ZMRg.opu.7afdnkmRW8e8Y4qj2j0kxK8J/jH/L6', 'EMAIL', '테스트사용자4', 0, false, NULL, 0, NOW(), NOW()),
      ('test5@toktot.com', '$2a$10$N9qo8uLOickgx2ZMRg.opu.7afdnkmRW8e8Y4qj2j0kxK8J/jH/L6', 'EMAIL', '테스트사용자5', 0, false, NULL, 0, NOW(), NOW()),
      ('test6@toktot.com', '$2a$10$N9qo8uLOickgx2ZMRg.opu.7afdnkmRW8e8Y4qj2j0kxK8J/jH/L6', 'EMAIL', '테스트사용자6', 0, false, NULL, 0, NOW(), NOW()),
      ('test7@toktot.com', '$2a$10$N9qo8uLOickgx2ZMRg.opu.7afdnkmRW8e8Y4qj2j0kxK8J/jH/L6', 'EMAIL', '테스트사용자7', 0, false, NULL, 0, NOW(), NOW()),
      ('test8@toktot.com', '$2a$10$N9qo8uLOickgx2ZMRg.opu.7afdnkmRW8e8Y4qj2j0kxK8J/jH/L6', 'EMAIL', '테스트사용자8', 0, false, NULL, 0, NOW(), NOW()),
      ('test9@toktot.com', '$2a$10$N9qo8uLOickgx2ZMRg.opu.7afdnkmRW8e8Y4qj2j0kxK8J/jH/L6', 'EMAIL', '테스트사용자9', 0, false, NULL, 0, NOW(), NOW()),
      ('test10@toktot.com', '$2a$10$N9qo8uLOickgx2ZMRg.opu.7afdnkmRW8e8Y4qj2j0kxK8J/jH/L6', 'EMAIL', '테스트사용자10', 0, false, NULL, 0, NOW(), NOW());

select * from users;
select * from reviews;