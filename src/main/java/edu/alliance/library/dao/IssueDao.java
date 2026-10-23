package edu.alliance.library.dao;

import edu.alliance.library.util.Database;
import edu.alliance.library.util.InputValidator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IssueDao {
    private final SettingsDao settingsDao = new SettingsDao();

    public void issue(long bookId, long studentRecordId, long staffUserId) throws SQLException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement("SELECT available_copies,reference_only FROM books WHERE id=? FOR UPDATE")) {
                    p.setLong(1, bookId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("Book not found.");
                        if (r.getBoolean("reference_only")) throw new IllegalArgumentException("Reference books cannot be borrowed.");
                        if (r.getInt("available_copies") < 1) throw new IllegalArgumentException("No copies are currently available.");
                    }
                }
                try (PreparedStatement p = c.prepareStatement("SELECT id FROM students WHERE id=? FOR UPDATE")) {
                    p.setLong(1, studentRecordId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("Student not found.");
                    }
                }
                int limit = Integer.parseInt(settingsDao.get(c, "borrowing_limit", "5"));
                try (PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM book_issues WHERE student_id=? AND returned=FALSE")) {
                    p.setLong(1, studentRecordId);
                    try (ResultSet r = p.executeQuery()) {
                        r.next();
                        if (r.getInt(1) >= limit) throw new IllegalArgumentException("This student has reached the borrowing limit.");
                    }
                }
                int days = Integer.parseInt(settingsDao.get(c, "loan_days", "14"));
                try (PreparedStatement p = c.prepareStatement("UPDATE books SET available_copies=available_copies-1 WHERE id=? AND available_copies>0")) {
                    p.setLong(1, bookId);
                    if (p.executeUpdate() != 1) throw new IllegalArgumentException("No copies are currently available.");
                }
                LocalDate issued = LocalDate.now();
                try (PreparedStatement p = c.prepareStatement("INSERT INTO book_issues(book_id,student_id,issued_by,issue_date,due_date) VALUES(?,?,?,?,?)")) {
                    p.setLong(1, bookId);
                    p.setLong(2, studentRecordId);
                    p.setLong(3, staffUserId);
                    p.setDate(4, Date.valueOf(issued));
                    p.setDate(5, Date.valueOf(issued.plusDays(days)));
                    p.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public BigDecimal returnBook(long issueId, long staffUserId) throws SQLException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                long bookId;
                LocalDate dueDate;
                boolean returned;
                try (PreparedStatement p = c.prepareStatement("SELECT book_id,due_date,returned FROM book_issues WHERE id=? FOR UPDATE")) {
                    p.setLong(1, issueId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) throw new IllegalArgumentException("Issue transaction not found.");
                        bookId = r.getLong("book_id");
                        dueDate = r.getDate("due_date").toLocalDate();
                        returned = r.getBoolean("returned");
                    }
                }
                if (returned) throw new IllegalArgumentException("This book has already been returned.");
                Map<String, String> settings = settingsDao.allInConnection(c);
                BigDecimal dailyRate = new BigDecimal(settings.getOrDefault("fine_per_day", "5.00"));
                long overdueDays = Math.max(0, ChronoUnit.DAYS.between(dueDate, LocalDate.now()));
                BigDecimal fine = dailyRate.multiply(BigDecimal.valueOf(overdueDays));
                try (PreparedStatement p = c.prepareStatement("UPDATE book_issues SET returned=TRUE WHERE id=? AND returned=FALSE")) {
                    p.setLong(1, issueId);
                    if (p.executeUpdate() != 1) throw new IllegalArgumentException("This book has already been returned.");
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE books SET available_copies=available_copies+1 WHERE id=? AND available_copies<total_copies")) {
                    p.setLong(1, bookId);
                    if (p.executeUpdate() != 1) throw new SQLException("Inventory could not be updated");
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO book_returns(issue_id,returned_by,return_date,fine_amount) VALUES(?,?,?,?)")) {
                    p.setLong(1, issueId);
                    p.setLong(2, staffUserId);
                    p.setDate(3, Date.valueOf(LocalDate.now()));
                    p.setBigDecimal(4, fine);
                    p.executeUpdate();
                }

                c.commit();
                return fine;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public List<Map<String, Object>> activeIssues(String query) throws SQLException {
        String sql = """
                SELECT i.id,i.issue_date,i.due_date,b.title,b.isbn,s.student_id,u.full_name
                FROM book_issues i JOIN books b ON b.id=i.book_id
                JOIN students s ON s.id=i.student_id JOIN users u ON u.id=s.user_id
                WHERE i.returned=FALSE AND (?='' OR LOWER(b.title) LIKE ? OR LOWER(s.student_id) LIKE ? OR LOWER(u.full_name) LIKE ?)
                ORDER BY i.due_date LIMIT 300
                """;
        String term = InputValidator.normalize(query);
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, term);
            p.setString(2, "%" + term + "%");
            p.setString(3, "%" + term + "%");
            p.setString(4, "%" + term + "%");
            try (ResultSet r = p.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (r.next()) {
                    LocalDate dueDate = r.getDate("due_date").toLocalDate();
                    long overdueDays = Math.max(0, ChronoUnit.DAYS.between(dueDate, LocalDate.now()));
                    rows.add(Map.of("id", r.getLong("id"), "issueDate", r.getDate("issue_date"),
                            "dueDate", r.getDate("due_date"), "title", r.getString("title"), "isbn", r.getString("isbn"),
                            "studentId", r.getString("student_id"), "studentName", r.getString("full_name"),
                            "overdueDays", overdueDays));
                }
                return rows;
            }
        }
    }

    public List<Map<String, Object>> history(long studentRecordId) throws SQLException {
        String sql = """
                SELECT i.id,b.title,b.author,i.issue_date,i.due_date,i.returned,r.return_date,r.fine_amount
                FROM book_issues i JOIN books b ON b.id=i.book_id
                LEFT JOIN book_returns r ON r.issue_id=i.id
                WHERE i.student_id=? ORDER BY i.issue_date DESC LIMIT 200
                """;
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, studentRecordId);
            try (ResultSet r = p.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (r.next()) rows.add(Map.of("id", r.getLong("id"), "title", r.getString("title"),
                        "author", r.getString("author"), "issueDate", r.getDate("issue_date"),
                        "dueDate", r.getDate("due_date"), "returned", r.getBoolean("returned"),
                        "returnDate", r.getDate("return_date") == null ? "" : r.getDate("return_date").toString(),
                        "fine", r.getBigDecimal("fine_amount") == null ? BigDecimal.ZERO : r.getBigDecimal("fine_amount")));
                return rows;
            }
        }
    }
}
