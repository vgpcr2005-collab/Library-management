package edu.alliance.library.dao;

import edu.alliance.library.util.Database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LibraryRequestDao {
    private final SettingsDao settings = new SettingsDao();

    public void requestBook(long studentRecordId, long bookId) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement book = connection.prepareStatement("SELECT reference_only FROM books WHERE id=?")) {
                    book.setLong(1, bookId);
                    try (ResultSet result = book.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Book not found.");
                        if (result.getBoolean("reference_only")) throw new IllegalArgumentException("Reference books cannot be requested for borrowing.");
                    }
                }
                ensureNoPending(connection, studentRecordId, bookId, "BORROW", null);
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO book_requests(student_id,book_id,request_type,status) VALUES(?,?,'BORROW','PENDING')")) {
                    insert.setLong(1, studentRecordId);
                    insert.setLong(2, bookId);
                    insert.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void requestRenewal(long studentRecordId, long issueId) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long bookId;
                try (PreparedStatement issue = connection.prepareStatement(
                        "SELECT book_id,due_date,returned FROM book_issues WHERE id=? AND student_id=? FOR UPDATE")) {
                    issue.setLong(1, issueId);
                    issue.setLong(2, studentRecordId);
                    try (ResultSet result = issue.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Loan not found.");
                        if (result.getBoolean("returned")) throw new IllegalArgumentException("A returned book cannot be renewed.");
                        if (result.getDate("due_date").toLocalDate().isBefore(LocalDate.now())) {
                            throw new IllegalArgumentException("Overdue books cannot be renewed online. Please contact library staff.");
                        }
                        bookId = result.getLong("book_id");
                    }
                }
                try (PreparedStatement priorRenewal = connection.prepareStatement(
                        "SELECT COUNT(*) FROM book_requests WHERE issue_id=? AND request_type='RENEW' AND status='APPROVED'")) {
                    priorRenewal.setLong(1, issueId);
                    try (ResultSet result = priorRenewal.executeQuery()) {
                        result.next();
                        if (result.getInt(1) > 0) throw new IllegalArgumentException("This loan has already been renewed.");
                    }
                }
                ensureNoPending(connection, studentRecordId, bookId, "RENEW", issueId);
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO book_requests(student_id,book_id,issue_id,request_type,status) VALUES(?,?,?,'RENEW','PENDING')")) {
                    insert.setLong(1, studentRecordId);
                    insert.setLong(2, bookId);
                    insert.setLong(3, issueId);
                    insert.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public List<Map<String, Object>> pending() throws SQLException {
        return requests("WHERE r.status='PENDING' ORDER BY r.requested_at");
    }

    public List<Map<String, Object>> forStudent(long studentRecordId) throws SQLException {
        return requests("WHERE r.student_id=? ORDER BY r.requested_at DESC", studentRecordId);
    }

    public void process(long requestId, long staffUserId, boolean approve) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long studentId;
                long bookId;
                long issueId;
                String type;
                try (PreparedStatement request = connection.prepareStatement(
                        "SELECT student_id,book_id,issue_id,request_type,status FROM book_requests WHERE id=? FOR UPDATE")) {
                    request.setLong(1, requestId);
                    try (ResultSet result = request.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Request not found.");
                        if (!"PENDING".equals(result.getString("status"))) {
                            throw new IllegalArgumentException("This request has already been processed.");
                        }
                        studentId = result.getLong("student_id");
                        bookId = result.getLong("book_id");
                        issueId = result.getLong("issue_id");
                        if (result.wasNull()) issueId = 0;
                        type = result.getString("request_type");
                    }
                }
                if (approve) {
                    if ("BORROW".equals(type)) approveBorrow(connection, studentId, bookId, staffUserId);
                    else approveRenewal(connection, studentId, issueId);
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE book_requests SET status=?,processed_by=?,processed_at=CURRENT_TIMESTAMP WHERE id=? AND status='PENDING'")) {
                    update.setString(1, approve ? "APPROVED" : "REJECTED");
                    update.setLong(2, staffUserId);
                    update.setLong(3, requestId);
                    if (update.executeUpdate() != 1) throw new IllegalArgumentException("This request has already been processed.");
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void approveBorrow(Connection connection, long studentRecordId, long bookId, long staffUserId)
            throws SQLException {
        try (PreparedStatement book = connection.prepareStatement(
                "SELECT available_copies,reference_only FROM books WHERE id=? FOR UPDATE")) {
            book.setLong(1, bookId);
            try (ResultSet result = book.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Book not found.");
                if (result.getBoolean("reference_only")) throw new IllegalArgumentException("Reference books cannot be borrowed.");
                if (result.getInt("available_copies") < 1) throw new IllegalArgumentException("No copies are currently available.");
            }
        }
        try (PreparedStatement student = connection.prepareStatement("SELECT id FROM students WHERE id=? FOR UPDATE")) {
            student.setLong(1, studentRecordId);
            try (ResultSet result = student.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Student not found.");
            }
        }
        int limit = Integer.parseInt(settings.get(connection, "borrowing_limit", "5"));
        try (PreparedStatement active = connection.prepareStatement(
                "SELECT COUNT(*) FROM book_issues WHERE student_id=? AND returned=FALSE")) {
            active.setLong(1, studentRecordId);
            try (ResultSet result = active.executeQuery()) {
                result.next();
                if (result.getInt(1) >= limit) throw new IllegalArgumentException("This student has reached the borrowing limit.");
            }
        }
        int days = Integer.parseInt(settings.get(connection, "loan_days", "14"));
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE books SET available_copies=available_copies-1 WHERE id=? AND available_copies>0")) {
            update.setLong(1, bookId);
            if (update.executeUpdate() != 1) throw new IllegalArgumentException("No copies are currently available.");
        }
        LocalDate issued = LocalDate.now();
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO book_issues(book_id,student_id,issued_by,issue_date,due_date) VALUES(?,?,?,?,?)")) {
            insert.setLong(1, bookId);
            insert.setLong(2, studentRecordId);
            insert.setLong(3, staffUserId);
            insert.setDate(4, Date.valueOf(issued));
            insert.setDate(5, Date.valueOf(issued.plusDays(days)));
            insert.executeUpdate();
        }
    }

    private void approveRenewal(Connection connection, long studentRecordId, long issueId) throws SQLException {
        LocalDate dueDate;
        try (PreparedStatement issue = connection.prepareStatement(
                "SELECT due_date,returned FROM book_issues WHERE id=? AND student_id=? FOR UPDATE")) {
            issue.setLong(1, issueId);
            issue.setLong(2, studentRecordId);
            try (ResultSet result = issue.executeQuery()) {
                if (!result.next() || result.getBoolean("returned")) throw new IllegalArgumentException("This loan is no longer active.");
                dueDate = result.getDate("due_date").toLocalDate();
            }
        }
        if (dueDate.isBefore(LocalDate.now())) throw new IllegalArgumentException("Overdue books cannot be renewed.");
        try (PreparedStatement priorRenewal = connection.prepareStatement(
                "SELECT COUNT(*) FROM book_requests WHERE issue_id=? AND request_type='RENEW' AND status='APPROVED'")) {
            priorRenewal.setLong(1, issueId);
            try (ResultSet result = priorRenewal.executeQuery()) {
                result.next();
                if (result.getInt(1) > 0) throw new IllegalArgumentException("This loan has already been renewed.");
            }
        }
        int days = Integer.parseInt(settings.get(connection, "loan_days", "14"));
        try (PreparedStatement update = connection.prepareStatement("UPDATE book_issues SET due_date=? WHERE id=?")) {
            update.setDate(1, Date.valueOf(dueDate.plusDays(days)));
            update.setLong(2, issueId);
            if (update.executeUpdate() != 1) throw new IllegalArgumentException("Loan could not be renewed.");
        }
    }

    private void ensureNoPending(Connection connection, long studentId, long bookId, String type, Long issueId)
            throws SQLException {
        String sql = issueId == null
                ? "SELECT id FROM book_requests WHERE student_id=? AND book_id=? AND request_type=? AND status='PENDING'"
                : "SELECT id FROM book_requests WHERE student_id=? AND book_id=? AND request_type=? AND issue_id=? AND status='PENDING'";
        try (PreparedStatement request = connection.prepareStatement(sql)) {
            request.setLong(1, studentId);
            request.setLong(2, bookId);
            request.setString(3, type);
            if (issueId != null) request.setLong(4, issueId);
            try (ResultSet result = request.executeQuery()) {
                if (result.next()) throw new IllegalArgumentException("You already have a pending request for this book.");
            }
        }
    }

    private List<Map<String, Object>> requests(String condition, Object... values) throws SQLException {
        String sql = """
                SELECT r.id,r.student_id,r.book_id,r.issue_id,r.request_type,r.status,r.requested_at,
                       b.title,b.author,s.student_id AS campus_id,u.full_name
                FROM book_requests r JOIN books b ON b.id=r.book_id
                JOIN students s ON s.id=r.student_id JOIN users u ON u.id=s.user_id
                """ + condition;
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            try (ResultSet result = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (result.next()) rows.add(Map.of("id", result.getLong("id"),
                        "requestType", result.getString("request_type"), "status", result.getString("status"),
                        "requestedAt", result.getTimestamp("requested_at"), "title", result.getString("title"),
                        "author", result.getString("author"), "studentName", result.getString("full_name"),
                        "studentId", result.getString("campus_id")));
                return rows;
            }
        }
    }
}
