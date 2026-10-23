package edu.alliance.library.dao;

import edu.alliance.library.model.StaffAccount;
import edu.alliance.library.util.Database;
import edu.alliance.library.util.InputValidator;
import edu.alliance.library.util.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AuthDao {
    public StaffAccount authenticate(String email, char[] password) throws SQLException {
        String sql = "SELECT id,email,full_name,role,password_hash,enabled FROM users WHERE LOWER(email)=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeEmail(email));
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    PasswordHasher.verify(password, dummyHash());
                    return null;
                }
                String role = result.getString("role");
                String hash = result.getString("password_hash");
                boolean enabled = result.getBoolean("enabled");
                boolean passwordValid = PasswordHasher.verify(password, hash == null ? dummyHash() : hash);
                if (!enabled || !("ADMIN".equals(role) || "LIBRARIAN".equals(role)) || !passwordValid) {
                    return null;
                }
                return new StaffAccount(result.getLong("id"), result.getString("email"),
                        result.getString("full_name"), role);
            }
        }
    }

    public void bootstrapAdmin(String email, char[] password) throws SQLException {
        String normalizedEmail = normalizeEmail(email);
        if (!InputValidator.validEmail(normalizedEmail) || password == null
                || password.length < 14 || password.length > 128) {
            throw new IllegalArgumentException("The bootstrap administrator email and a password of 14–128 characters are required.");
        }
        String passwordHash = PasswordHasher.hash(password);
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement find = connection.prepareStatement(
                    "SELECT id,role,password_hash FROM users WHERE LOWER(email)=? FOR UPDATE")) {
                find.setString(1, normalizedEmail);
                try (ResultSet result = find.executeQuery()) {
                    if (result.next()) {
                        String role = result.getString("role");
                        String existingHash = result.getString("password_hash");
                        if (!"ADMIN".equals(role)) {
                            throw new IllegalStateException("Bootstrap email already belongs to a non-administrator account.");
                        }
                        if (existingHash != null) {
                            connection.commit();
                            return;
                        }
                        try (PreparedStatement update = connection.prepareStatement(
                                "UPDATE users SET password_hash=?,enabled=TRUE,full_name=? WHERE id=?")) {
                            update.setString(1, passwordHash);
                            update.setString(2, "Library Administrator");
                            update.setLong(3, result.getLong("id"));
                            update.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement insert = connection.prepareStatement(
                                "INSERT INTO users(email,password_hash,role,full_name,enabled) VALUES(?,?,'ADMIN',?,TRUE)")) {
                            insert.setString(1, normalizedEmail);
                            insert.setString(2, passwordHash);
                            insert.setString(3, "Library Administrator");
                            insert.executeUpdate();
                        }
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void createLibrarian(String email, String fullName, char[] password) throws SQLException {
        String normalizedEmail = normalizeEmail(email);
        String normalizedName = fullName == null ? "" : fullName.trim();
        if (!InputValidator.validEmail(normalizedEmail) || normalizedName.isBlank() || normalizedName.length() > 120
                || password == null || password.length < 14 || password.length > 128) {
            throw new IllegalArgumentException("Enter a valid email, name, and password of 14–128 characters.");
        }
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users(email,password_hash,role,full_name,enabled) VALUES(?,?,'LIBRARIAN',?,TRUE)")) {
            statement.setString(1, normalizedEmail);
            statement.setString(2, PasswordHasher.hash(password));
            statement.setString(3, normalizedName);
            statement.executeUpdate();
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new IllegalArgumentException("An account with that email already exists.");
            }
            throw e;
        }
    }

    public void setEnabled(long accountId, boolean enabled) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET enabled=? WHERE id=? AND role='LIBRARIAN'")) {
            statement.setBoolean(1, enabled);
            statement.setLong(2, accountId);
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("Librarian account was not found.");
        }
    }

    public List<Map<String, Object>> listStaff() throws SQLException {
        String sql = "SELECT id,email,full_name,role,enabled FROM users WHERE role IN ('ADMIN','LIBRARIAN') AND password_hash IS NOT NULL ORDER BY role,full_name";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<Map<String, Object>> accounts = new ArrayList<>();
            while (result.next()) {
                accounts.add(Map.of("id", result.getLong("id"), "email", result.getString("email"),
                        "name", result.getString("full_name"), "role", result.getString("role"),
                        "enabled", result.getBoolean("enabled")));
            }
            return accounts;
        }
    }

    public boolean hasEnabledStaff() throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM users WHERE enabled=TRUE AND password_hash IS NOT NULL AND role IN ('ADMIN','LIBRARIAN') LIMIT 1");
             ResultSet result = statement.executeQuery()) {
            return result.next();
        }
    }

    public boolean isEnabledStaff(long accountId) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM users WHERE id=? AND enabled=TRUE AND password_hash IS NOT NULL AND role IN ('ADMIN','LIBRARIAN')")) {
            statement.setLong(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public void changePassword(long accountId, char[] currentPassword, char[] newPassword) throws SQLException {
        if (newPassword == null || newPassword.length < 14 || newPassword.length > 128) {
            throw new IllegalArgumentException("Your new password must contain between 14 and 128 characters.");
        }
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT password_hash FROM users WHERE id=? AND role IN ('ADMIN','LIBRARIAN') AND enabled=TRUE FOR UPDATE")) {
                select.setLong(1, accountId);
                try (ResultSet result = select.executeQuery()) {
                    if (!result.next() || !PasswordHasher.verify(currentPassword, result.getString("password_hash"))) {
                        throw new IllegalArgumentException("Current password is incorrect.");
                    }
                }
                try (PreparedStatement update = connection.prepareStatement("UPDATE users SET password_hash=? WHERE id=?")) {
                    update.setString(1, PasswordHasher.hash(newPassword));
                    update.setLong(2, accountId);
                    update.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String dummyHash() {
        return "pbkdf2-sha256$310000$MDEyMzQ1Njc4OWFiY2RlZg$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    }
}
