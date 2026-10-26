USE alliance_library;

INSERT INTO books (isbn, title, author, category, total_copies, available_copies, reference_only) VALUES
('9780132350884', 'Clean Code', 'Robert C. Martin', 'Software Engineering', 8, 8, FALSE),
('9780262033848', 'Introduction to Algorithms', 'Thomas H. Cormen', 'Computer Science', 5, 5, FALSE),
('9780073523323', 'Database System Concepts', 'Abraham Silberschatz', 'Databases', 6, 6, FALSE),
('9780199535569', 'The Oxford English Dictionary', 'Oxford University Press', 'Reference', 2, 2, TRUE),
('9780134494166', 'The Linux Programming Interface', 'Michael Kerrisk', 'Operating Systems', 4, 4, FALSE),
('9780134685991', 'Effective Java', 'Joshua Bloch', 'Software Engineering', 5, 5, FALSE),
('9781449373320', 'Designing Data-Intensive Applications', 'Martin Kleppmann', 'Databases', 4, 4, FALSE),
('9780596007126', 'Head First Design Patterns', 'Eric Freeman and Elisabeth Robson', 'Software Engineering', 3, 3, FALSE),
('9780135957059', 'The Pragmatic Programmer', 'David Thomas and Andrew Hunt', 'Software Engineering', 4, 4, FALSE),
('9781593279288', 'Python Crash Course', 'Eric Matthes', 'Programming', 5, 5, FALSE),
('9780061120084', 'To Kill a Mockingbird', 'Harper Lee', 'Literature', 4, 4, FALSE),
('9780743273565', 'The Great Gatsby', 'F. Scott Fitzgerald', 'Literature', 4, 4, FALSE),
('9780141439600', 'Pride and Prejudice', 'Jane Austen', 'Literature', 4, 4, FALSE),
('9780451524935', 'Nineteen Eighty-Four', 'George Orwell', 'Literature', 4, 4, FALSE),
('9780140449136', 'The Odyssey', 'Homer', 'Classics', 3, 3, FALSE),
('9780140449266', 'Crime and Punishment', 'Fyodor Dostoevsky', 'Classics', 3, 3, FALSE),
('9780062316097', 'Sapiens', 'Yuval Noah Harari', 'History', 3, 3, FALSE),
('9780062464316', 'Homo Deus', 'Yuval Noah Harari', 'History', 2, 2, FALSE),
('9780131103627', 'The C Programming Language', 'Brian W. Kernighan and Dennis M. Ritchie', 'Programming', 3, 3, FALSE),
('9780136019701', 'Clean Architecture', 'Robert C. Martin', 'Software Engineering', 3, 3, FALSE),
('9780137081073', 'The Clean Coder', 'Robert C. Martin', 'Software Engineering', 3, 3, FALSE),
('9781491950357', 'Learning React', 'Alex Banks and Eve Porcello', 'Programming', 3, 3, FALSE),
('9780321125217', 'Domain-Driven Design', 'Eric Evans', 'Software Engineering', 2, 2, FALSE),
('9780201633610', 'Design Patterns', 'Erich Gamma, Richard Helm, Ralph Johnson, and John Vlissides', 'Software Engineering', 3, 3, FALSE),
('9780134757599', 'Refactoring', 'Martin Fowler', 'Software Engineering', 3, 3, FALSE)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- Demo student fixtures have no password and cannot sign in.
INSERT INTO users (email, password_hash, role, full_name, enabled) VALUES
('aarav.sharma@example.edu', NULL, 'STUDENT', 'Aarav Sharma', TRUE),
('diya.nair@example.edu', NULL, 'STUDENT', 'Diya Nair', TRUE)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO students (user_id, student_id, department, phone)
SELECT id, 'AU-DEMO-1001', 'Computer Science', '+91 90000 10001'
FROM users WHERE email = 'aarav.sharma@example.edu'
ON DUPLICATE KEY UPDATE department = VALUES(department), phone = VALUES(phone);

INSERT INTO students (user_id, student_id, department, phone)
SELECT id, 'AU-DEMO-1002', 'Business Administration', '+91 90000 10002'
FROM users WHERE email = 'diya.nair@example.edu'
ON DUPLICATE KEY UPDATE department = VALUES(department), phone = VALUES(phone);

-- Demo student fixtures are directory records only; students create their own sign-in accounts.
