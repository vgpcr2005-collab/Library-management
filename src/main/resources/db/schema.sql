CREATE DATABASE IF NOT EXISTS alliance_library
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE alliance_library;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NULL,
  role ENUM('STUDENT','LIBRARIAN','ADMIN') NOT NULL DEFAULT 'STUDENT',
  full_name VARCHAR(120) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NULL;

INSERT INTO users (email, password_hash, role, full_name, enabled)
VALUES ('local-library@localhost', NULL, 'ADMIN', 'Local Library Operator', TRUE)
ON DUPLICATE KEY UPDATE email = VALUES(email);

CREATE TABLE IF NOT EXISTS students (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  student_id VARCHAR(40) NOT NULL UNIQUE,
  department VARCHAR(100) NOT NULL,
  phone VARCHAR(24) NOT NULL,
  CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS books (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  isbn VARCHAR(20) NOT NULL UNIQUE,
  title VARCHAR(240) NOT NULL,
  author VARCHAR(160) NOT NULL,
  category VARCHAR(100) NOT NULL,
  total_copies INT NOT NULL,
  available_copies INT NOT NULL,
  reference_only BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_book_copies CHECK (total_copies >= 0 AND available_copies >= 0 AND available_copies <= total_copies),
  INDEX idx_books_title (title),
  INDEX idx_books_category (category)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS book_issues (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  book_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  issued_by BIGINT NOT NULL,
  issue_date DATE NOT NULL,
  due_date DATE NOT NULL,
  returned BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_issues_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE RESTRICT,
  CONSTRAINT fk_issues_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
  CONSTRAINT fk_issues_librarian FOREIGN KEY (issued_by) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT chk_issue_dates CHECK (due_date >= issue_date),
  INDEX idx_issues_active_student (student_id, returned),
  INDEX idx_issues_due (returned, due_date)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS book_returns (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  issue_id BIGINT NOT NULL UNIQUE,
  returned_by BIGINT NOT NULL,
  return_date DATE NOT NULL,
  fine_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  CONSTRAINT fk_returns_issue FOREIGN KEY (issue_id) REFERENCES book_issues(id) ON DELETE RESTRICT,
  CONSTRAINT fk_returns_user FOREIGN KEY (returned_by) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT chk_fine CHECK (fine_amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS book_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  issue_id BIGINT NULL,
  request_type ENUM('BORROW','RENEW') NOT NULL,
  status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_by BIGINT NULL,
  processed_at TIMESTAMP NULL,
  CONSTRAINT fk_requests_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
  CONSTRAINT fk_requests_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE RESTRICT,
  CONSTRAINT fk_requests_issue FOREIGN KEY (issue_id) REFERENCES book_issues(id) ON DELETE RESTRICT,
  CONSTRAINT fk_requests_user FOREIGN KEY (processed_by) REFERENCES users(id) ON DELETE RESTRICT,
  INDEX idx_book_requests_status (status, requested_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS book_return_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  issue_id BIGINT NOT NULL,
  status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_by BIGINT NULL,
  processed_at TIMESTAMP NULL,
  CONSTRAINT fk_return_requests_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
  CONSTRAINT fk_return_requests_issue FOREIGN KEY (issue_id) REFERENCES book_issues(id) ON DELETE RESTRICT,
  CONSTRAINT fk_return_requests_user FOREIGN KEY (processed_by) REFERENCES users(id) ON DELETE RESTRICT,
  INDEX idx_return_requests_status (status, requested_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS library_settings (
  setting_key VARCHAR(60) PRIMARY KEY,
  setting_value VARCHAR(120) NOT NULL
) ENGINE=InnoDB;

INSERT INTO library_settings (setting_key, setting_value) VALUES
  ('loan_days', '14'), ('fine_per_day', '5.00'), ('borrowing_limit', '5')
ON DUPLICATE KEY UPDATE setting_key = VALUES(setting_key);

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
