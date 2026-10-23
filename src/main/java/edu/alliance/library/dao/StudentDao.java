package edu.alliance.library.dao;

import edu.alliance.library.model.Student;
import edu.alliance.library.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentDao {
    public void update(long userId, long studentRecordId, String fullName, String email,
                       String studentId, String department, String phone) throws SQLException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement user = c.prepareStatement("UPDATE users SET full_name=?,email=? WHERE id=? AND role='STUDENT'");
                 PreparedStatement student = c.prepareStatement("UPDATE students SET student_id=?,department=?,phone=? WHERE id=? AND user_id=?")) {
                user.setString(1, fullName);
                user.setString(2, email);
                user.setLong(3, userId);
                if (user.executeUpdate() != 1) throw new SQLException("Student account was not found");
                student.setString(1, studentId);
                student.setString(2, department);
                student.setString(3, phone);
                student.setLong(4, studentRecordId);
                student.setLong(5, userId);
                if (student.executeUpdate() != 1) throw new SQLException("Student record was not found");
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                if ("23000".equals(e.getSQLState())) throw new IllegalArgumentException("That email or student ID is already registered.");
                throw e;
            }
        }
    }

    public List<Student> search(String query) throws SQLException {
        String sql = """
                SELECT u.id,u.email,u.full_name,s.id AS student_record_id,s.student_id,s.department,s.phone
                FROM students s JOIN users u ON u.id=s.user_id
                WHERE (?='' OR s.student_id LIKE ? OR u.full_name LIKE ?)
                ORDER BY u.full_name LIMIT 200
                """;
        String term = query == null ? "" : query.trim();
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, term);
            p.setString(2, "%" + term + "%");
            p.setString(3, "%" + term + "%");
            try (ResultSet r = p.executeQuery()) {
                List<Student> students = new ArrayList<>();
                while (r.next()) students.add(new Student(r.getLong("id"), r.getString("email"),
                        r.getString("full_name"), r.getLong("student_record_id"), r.getString("student_id"),
                        r.getString("department"), r.getString("phone")));
                return students;
            }
        }
    }

    public Student findByRecordId(long recordId) throws SQLException {
        String sql = "SELECT u.id,u.email,u.full_name,s.id AS student_record_id,s.student_id,s.department,s.phone " +
                "FROM students s JOIN users u ON u.id=s.user_id WHERE s.id=?";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, recordId);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? new Student(r.getLong("id"), r.getString("email"), r.getString("full_name"),
                        r.getLong("student_record_id"), r.getString("student_id"), r.getString("department"),
                        r.getString("phone")) : null;
            }
        }
    }
}
