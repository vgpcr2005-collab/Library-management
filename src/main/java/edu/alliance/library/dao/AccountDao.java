package edu.alliance.library.dao;

import edu.alliance.library.model.Account;
import edu.alliance.library.util.Database;
import edu.alliance.library.util.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountDao {
    public Account authenticate(String email, char[] password) throws SQLException {
        String sql = """
                SELECT u.id,u.email,u.full_name,u.role,u.password_hash,s.id AS student_record_id
                FROM users u LEFT JOIN students s ON s.user_id=u.id
                WHERE u.email=? AND u.enabled=TRUE
                """;
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !PasswordHasher.verify(password, result.getString("password_hash"))) return null;
                long studentRecordId = result.getLong("student_record_id");
                boolean missingStudentRecord = result.wasNull();
                return new Account(result.getLong("id"), result.getString("email"), result.getString("full_name"),
                        result.getString("role"), missingStudentRecord ? 0 : studentRecordId);
            }
        }
    }

    public Account registerStudent(String email, String fullName, String studentId, String department,
                                   String phone, char[] password) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long userId;
                try (PreparedStatement user = connection.prepareStatement(
                        "INSERT INTO users(email,password_hash,role,full_name,enabled) VALUES(?,?,'STUDENT',?,TRUE)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    user.setString(1, email);
                    user.setString(2, PasswordHasher.hash(password));
                    user.setString(3, fullName);
                    user.executeUpdate();
                    try (ResultSet keys = user.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Student account could not be created.");
                        userId = keys.getLong(1);
                    }
                }
                long recordId;
                try (PreparedStatement student = connection.prepareStatement(
                        "INSERT INTO students(user_id,student_id,department,phone) VALUES(?,?,?,?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    student.setLong(1, userId);
                    student.setString(2, studentId);
                    student.setString(3, department);
                    student.setString(4, phone);
                    student.executeUpdate();
                    try (ResultSet keys = student.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Student profile could not be created.");
                        recordId = keys.getLong(1);
                    }
                }
                connection.commit();
                return new Account(userId, email, fullName, "STUDENT", recordId);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                if ("23000".equals(e instanceof SQLException sqlException ? sqlException.getSQLState() : null)) {
                    throw new IllegalArgumentException("That email or student ID is already registered.");
                }
                throw e;
            }
        }
    }

    public void updateStudent(long userId, String fullName, String email, String department, String phone)
            throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement user = connection.prepareStatement(
                    "UPDATE users SET full_name=?,email=? WHERE id=? AND role='STUDENT'");
                 PreparedStatement student = connection.prepareStatement(
                         "UPDATE students SET department=?,phone=? WHERE user_id=?")) {
                user.setString(1, fullName);
                user.setString(2, email);
                user.setLong(3, userId);
                if (user.executeUpdate() != 1) throw new SQLException("Student account was not found.");
                student.setString(1, department);
                student.setString(2, phone);
                student.setLong(3, userId);
                if (student.executeUpdate() != 1) throw new SQLException("Student profile was not found.");
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                if ("23000".equals(e instanceof SQLException sqlException ? sqlException.getSQLState() : null)) {
                    throw new IllegalArgumentException("That email address is already registered.");
                }
                throw e;
            }
        }
    }
}
