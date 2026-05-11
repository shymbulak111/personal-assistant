-- Demo admin user (password: Admin1234!)
-- BCrypt hash of 'Admin1234!'
INSERT INTO users (email, password, first_name, last_name, role, ai_requests_limit)
VALUES ('admin@projem.kz', '$2a$10$QGeaG0n6UvTYnL7N5nRgBuTYIWuuJYLAsIjzyGIoRlKR6FY1yrEmy', 'Admin', 'User', 'ROLE_ADMIN', 1000)
ON CONFLICT (email) DO NOTHING;
