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

CREATE TABLE IF NOT EXISTS library_settings (
  setting_key VARCHAR(60) PRIMARY KEY,
  setting_value VARCHAR(120) NOT NULL
) ENGINE=InnoDB;

INSERT INTO library_settings (setting_key, setting_value) VALUES
  ('loan_days', '14'), ('fine_per_day', '5.00'), ('borrowing_limit', '5')
ON DUPLICATE KEY UPDATE setting_key = VALUES(setting_key);
