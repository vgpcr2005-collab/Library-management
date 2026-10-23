USE alliance_library;

INSERT INTO books (isbn, title, author, category, total_copies, available_copies, reference_only) VALUES
('9780132350884', 'Clean Code', 'Robert C. Martin', 'Software Engineering', 8, 8, FALSE),
('9780262033848', 'Introduction to Algorithms', 'Thomas H. Cormen', 'Computer Science', 5, 5, FALSE),
('9780073523323', 'Database System Concepts', 'Abraham Silberschatz', 'Databases', 6, 6, FALSE),
('9780199535569', 'The Oxford English Dictionary', 'Oxford University Press', 'Reference', 2, 2, TRUE),
('9780134494166', 'The Linux Programming Interface', 'Michael Kerrisk', 'Operating Systems', 4, 4, FALSE)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- Local-only student fixture records; public account login and registration are disabled.
INSERT INTO users (email, password_hash, role, full_name, enabled) VALUES
('aarav.sharma@example.edu', NULL, 'STUDENT', 'Aarav Sharma', TRUE),
('diya.nair@example.edu', NULL, 'STUDENT', 'Diya Nair', TRUE)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), password_hash = NULL;

INSERT INTO students (user_id, student_id, department, phone)
SELECT id, 'AU-DEMO-1001', 'Computer Science', '+91 90000 10001'
FROM users WHERE email = 'aarav.sharma@example.edu'
ON DUPLICATE KEY UPDATE department = VALUES(department), phone = VALUES(phone);

INSERT INTO students (user_id, student_id, department, phone)
SELECT id, 'AU-DEMO-1002', 'Business Administration', '+91 90000 10002'
FROM users WHERE email = 'diya.nair@example.edu'
ON DUPLICATE KEY UPDATE department = VALUES(department), phone = VALUES(phone);

-- Student records are library members only; this local app has no account login.
