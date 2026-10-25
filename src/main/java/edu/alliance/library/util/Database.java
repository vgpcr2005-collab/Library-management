package edu.alliance.library.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Database {
    private static final String DB_DIRECTORY = ".alliance-smart-library";
    private static volatile boolean embeddedInitialized;
    private static volatile boolean mySqlInitialized;
    private static final Pattern REQUIRED_SSL_MODE =
            Pattern.compile("(?i)(?:^|&)sslMode=(?:REQUIRED|VERIFY_CA|VERIFY_IDENTITY)(?:&|$)");

    private Database() { }

    public static Connection getConnection() throws SQLException {
        boolean production = isProductionRuntime();
        String url = environmentValue("LIBRARY_DB_URL");
        String user = environmentValue("LIBRARY_DB_USER");
        String password = System.getenv("LIBRARY_DB_PASSWORD");
        boolean anyMySqlSetting = url != null || user != null || password != null;
        if (anyMySqlSetting && (isBlank(url) || isBlank(user) || isBlank(password))) {
            String environment = production ? "Production database" : "Database";
            throw new SQLException(environment + " configuration is incomplete. Set LIBRARY_DB_URL, LIBRARY_DB_USER, and LIBRARY_DB_PASSWORD.", "08001");
        }
        if (production && !anyMySqlSetting) {
            throw new SQLException("Production database configuration is missing. Set LIBRARY_DB_URL, LIBRARY_DB_USER, and LIBRARY_DB_PASSWORD.", "08001");
        }
        if (anyMySqlSetting) {
            if (!url.startsWith("jdbc:mysql://")) {
                throw new SQLException("LIBRARY_DB_URL must use the jdbc:mysql:// scheme.", "08001");
            }
            if (production) validateProductionUrl(url);
            return getMySqlConnection(url, user, password);
        }

        String embeddedPath = System.getProperty("alliance.library.database.path");
        return getEmbeddedConnection(embeddedPath);
    }

    public static boolean isProductionRuntime() {
        String appEnvironment = System.getenv("APP_ENV");
        return "production".equalsIgnoreCase(appEnvironment)
                || !isBlank(System.getenv("RENDER_SERVICE_ID"))
                || "true".equalsIgnoreCase(System.getenv("RENDER"));
    }

    public static String failureMessage(SQLException exception) {
        String message = exception.getMessage();
        if (message != null && message.startsWith("Production database configuration")) {
            return "Production database configuration is missing. Set LIBRARY_DB_URL, LIBRARY_DB_USER, and LIBRARY_DB_PASSWORD in Render, then restart the service.";
        }
        if (message != null && message.startsWith("Database configuration is incomplete")) {
            return "Database configuration is incomplete. Set LIBRARY_DB_URL, LIBRARY_DB_USER, and LIBRARY_DB_PASSWORD, or remove all three to use the local database.";
        }
        if (message != null && message.startsWith("LIBRARY_DB_URL")) {
            return "LIBRARY_DB_URL must contain the hosted MySQL hostname, port, database name, and a required SSL mode.";
        }
        boolean mysqlConfigured = !isBlank(System.getenv("LIBRARY_DB_URL"));
        if (!isProductionRuntime() && !mysqlConfigured) {
            if (isConnectionFailure(exception)) {
                return "The local library database could not be opened. Check that your Windows account can write to its .alliance-smart-library folder, then restart Tomcat.";
            }
            return "The local library database could not be initialized. Check the application log, then restart the app.";
        }
        String sqlState = exception.getSQLState();
        if (sqlState != null && sqlState.startsWith("28")) {
            return "The configured MySQL server rejected the configured credentials. Check LIBRARY_DB_USER and LIBRARY_DB_PASSWORD.";
        }
        if (isConnectionFailure(exception)) {
            return "The configured MySQL database could not be reached. Check the hosted hostname, port, database name, SSL mode, credentials, and database network access.";
        }
        return "The MySQL schema could not be initialized. Confirm the database exists and the app user can create and alter its tables without deleting existing data.";
    }

    public static String safeDiagnostic(SQLException exception) {
        String state = exception.getSQLState();
        if (state == null || !state.matches("[A-Z0-9]{5}")) state = "unknown";
        return "(SQLState=" + state + ", vendorCode=" + exception.getErrorCode() + ")";
    }

    private static boolean isConnectionFailure(SQLException exception) {
        String state = exception.getSQLState();
        return state != null && state.startsWith("08");
    }

    static void validateProductionUrl(String url) throws SQLException {
        try {
            URI uri = URI.create(url.substring("jdbc:".length()));
            String path = uri.getPath();
            if (uri.getHost() == null || uri.getPort() < 1 || path == null || path.length() < 2
                    || uri.getQuery() == null || !REQUIRED_SSL_MODE.matcher(uri.getQuery()).find()) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new SQLException(
                    "LIBRARY_DB_URL must contain a hosted MySQL hostname, explicit port, database name, and sslMode=REQUIRED, VERIFY_CA, or VERIFY_IDENTITY.",
                    "08001");
        }
    }

    private static Connection getMySqlConnection(String url, String user, String password) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("The MySQL JDBC driver is not available to the application.", "08001");
        }
        Connection connection;
        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new SQLException("Could not connect to the configured production MySQL database.", e.getSQLState(), e.getErrorCode());
        }
        try {
            initializeMySqlDatabase(connection);
            return connection;
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                e.addSuppressed(closeException);
            }
            throw new SQLException("Could not safely initialize the production MySQL schema.", e.getSQLState(), e.getErrorCode());
        }
    }

    private static void initializeMySqlDatabase(Connection connection) throws SQLException {
        if (mySqlInitialized) return;
        synchronized (Database.class) {
            if (mySqlInitialized) return;
            try (InputStream schema = Database.class.getClassLoader().getResourceAsStream("db/schema.sql")) {
                if (schema == null) {
                    throw new SQLException("The MySQL schema resource is missing.", "HY000");
                }
                StringBuilder script = new StringBuilder();
                for (String line : new String(schema.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                    if (!line.stripLeading().startsWith("--")) script.append(line).append('\n');
                }
                try (var statement = connection.createStatement()) {
                    for (String sql : script.toString().split(";")) {
                        String command = sql.trim();
                        String normalized = command.toUpperCase(Locale.ROOT);
                        if (command.isEmpty() || normalized.startsWith("CREATE DATABASE")
                                || normalized.startsWith("USE ")) continue;
                        statement.execute(command);
                    }
                }
                ensureBootstrapStaff(connection);
                mySqlInitialized = true;
            } catch (IOException e) {
                throw new SQLException("The MySQL schema resource could not be read.", "HY000");
            }
        }
    }

    private static Connection getEmbeddedConnection(String configuredPath) throws SQLException {
        Path databasePath;
        try {
            if (isBlank(configuredPath)) {
                Path directory = Path.of(System.getProperty("user.home"), DB_DIRECTORY);
                Files.createDirectories(directory);
                databasePath = directory.resolve("library").toAbsolutePath();
            } else {
                databasePath = Path.of(configuredPath).toAbsolutePath();
                Files.createDirectories(databasePath.getParent());
            }
        } catch (RuntimeException | java.io.IOException e) {
            throw new SQLException("The local library database folder could not be opened.", "08001", e);
        }

        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("The embedded library database driver is not available.", "08001", e);
        }

        String normalizedPath = databasePath.toString().replace('\\', '/');
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:file:" + normalizedPath + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        try {
            initializeEmbeddedDatabase(connection);
            return connection;
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
    }

    private static void initializeEmbeddedDatabase(Connection connection) throws SQLException {
        if (embeddedInitialized) return;
        synchronized (Database.class) {
            if (embeddedInitialized) return;
            try (var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                          id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                          email VARCHAR(254) NOT NULL UNIQUE,
                          password_hash VARCHAR(255),
                          role VARCHAR(20) NOT NULL DEFAULT 'STUDENT'
                            CHECK (role IN ('STUDENT','LIBRARIAN','ADMIN')),
                          full_name VARCHAR(120) NOT NULL,
                          enabled BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS students (
                          id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                          user_id BIGINT NOT NULL UNIQUE,
                          student_id VARCHAR(40) NOT NULL UNIQUE,
                          department VARCHAR(100) NOT NULL,
                          phone VARCHAR(24) NOT NULL,
                          CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS books (
                          id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                          isbn VARCHAR(20) NOT NULL UNIQUE,
                          title VARCHAR(240) NOT NULL,
                          author VARCHAR(160) NOT NULL,
                          category VARCHAR(100) NOT NULL,
                          total_copies INT NOT NULL,
                          available_copies INT NOT NULL,
                          reference_only BOOLEAN NOT NULL DEFAULT FALSE,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT chk_book_copies CHECK (total_copies >= 0 AND available_copies >= 0 AND available_copies <= total_copies)
                        )
                        """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_books_title ON books(title)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_books_category ON books(category)");
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS book_issues (
                          id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                          book_id BIGINT NOT NULL,
                          student_id BIGINT NOT NULL,
                          issued_by BIGINT NOT NULL,
                          issue_date DATE NOT NULL,
                          due_date DATE NOT NULL,
                          returned BOOLEAN NOT NULL DEFAULT FALSE,
                          CONSTRAINT fk_issues_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE RESTRICT,
                          CONSTRAINT fk_issues_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
                          CONSTRAINT fk_issues_librarian FOREIGN KEY (issued_by) REFERENCES users(id) ON DELETE RESTRICT,
                          CONSTRAINT chk_issue_dates CHECK (due_date >= issue_date)
                        )
                        """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_issues_active_student ON book_issues(student_id, returned)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_issues_due ON book_issues(returned, due_date)");
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS book_requests (
                          id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                          student_id BIGINT NOT NULL,
                          book_id BIGINT NOT NULL,
                          issue_id BIGINT,
                          request_type VARCHAR(10) NOT NULL CHECK (request_type IN ('BORROW','RENEW')),
                          status VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
                          requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          processed_by BIGINT,
                          processed_at TIMESTAMP,
                          CONSTRAINT fk_requests_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
                          CONSTRAINT fk_requests_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE RESTRICT,
                          CONSTRAINT fk_requests_issue FOREIGN KEY (issue_id) REFERENCES book_issues(id) ON DELETE RESTRICT,
                          CONSTRAINT fk_requests_user FOREIGN KEY (processed_by) REFERENCES users(id) ON DELETE RESTRICT
                        )
                        """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_book_requests_status ON book_requests(status, requested_at)");
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS book_returns (
                          id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                          issue_id BIGINT NOT NULL UNIQUE,
                          returned_by BIGINT NOT NULL,
                          return_date DATE NOT NULL,
                          fine_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
                          CONSTRAINT fk_returns_issue FOREIGN KEY (issue_id) REFERENCES book_issues(id) ON DELETE RESTRICT,
                          CONSTRAINT fk_returns_user FOREIGN KEY (returned_by) REFERENCES users(id) ON DELETE RESTRICT,
                          CONSTRAINT chk_fine CHECK (fine_amount >= 0)
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS library_settings (
                          setting_key VARCHAR(60) PRIMARY KEY,
                          setting_value VARCHAR(120) NOT NULL
                        )
                        """);
            }
            seedLocalData(connection);
            ensureBootstrapStaff(connection);
            embeddedInitialized = true;
        }
    }

    private static void ensureBootstrapStaff(Connection connection) throws SQLException {
        String email = environmentValue("LIBRARY_STAFF_EMAIL");
        String passwordValue = System.getenv("LIBRARY_STAFF_PASSWORD");
        if (email == null && isBlank(passwordValue)) return;
        if (email == null || isBlank(passwordValue)) {
            throw new SQLException("Set both LIBRARY_STAFF_EMAIL and LIBRARY_STAFF_PASSWORD to provision the initial staff account.", "08001");
        }
        email = email.toLowerCase(Locale.ROOT);
        if (!InputValidator.validEmail(email) || passwordValue.length() < 12 || passwordValue.length() > 128) {
            throw new SQLException("The initial staff account requires a valid email and a password between 12 and 128 characters.", "08001");
        }
        try (PreparedStatement find = connection.prepareStatement("SELECT role,password_hash FROM users WHERE email=?")) {
            find.setString(1, email);
            try (ResultSet result = find.executeQuery()) {
                if (result.next()) {
                    String role = result.getString("role");
                    if (!"LIBRARIAN".equals(role) && !"ADMIN".equals(role)) {
                        throw new SQLException("LIBRARY_STAFF_EMAIL belongs to an account that is not a staff account.", "23000");
                    }
                    if (result.getString("password_hash") == null) {
                        char[] password = passwordValue.toCharArray();
                        try (PreparedStatement update = connection.prepareStatement(
                                "UPDATE users SET password_hash=? WHERE email=? AND password_hash IS NULL")) {
                            update.setString(1, PasswordHasher.hash(password));
                            update.setString(2, email);
                            update.executeUpdate();
                        } finally {
                            java.util.Arrays.fill(password, '\0');
                        }
                    }
                    return;
                }
            }
        }
        char[] password = passwordValue.toCharArray();
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO users(email,password_hash,role,full_name,enabled) VALUES(?,?,'LIBRARIAN',?,TRUE)")) {
            insert.setString(1, email);
            insert.setString(2, PasswordHasher.hash(password));
            insert.setString(3, "Library Staff");
            insert.executeUpdate();
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private static void seedLocalData(Connection connection) throws SQLException {
        ensureUser(connection, "local-library@localhost", "ADMIN", "Local Library Operator");

        String[][] students = {
                {"aarav.sharma@example.edu", "Aarav Sharma", "AU-DEMO-1001", "Computer Science", "+91 90000 10001"},
                {"diya.nair@example.edu", "Diya Nair", "AU-DEMO-1002", "Business Administration", "+91 90000 10002"}
        };
        for (String[] student : students) {
            ensureUser(connection, student[0], "STUDENT", student[1]);
            long userId = userId(connection, student[0]);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM students WHERE user_id=? OR student_id=?")) {
                statement.setLong(1, userId);
                statement.setString(2, student[2]);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        try (PreparedStatement insert = connection.prepareStatement(
                                "INSERT INTO students(user_id,student_id,department,phone) VALUES(?,?,?,?)")) {
                            insert.setLong(1, userId);
                            insert.setString(2, student[2]);
                            insert.setString(3, student[3]);
                            insert.setString(4, student[4]);
                            insert.executeUpdate();
                        }
                    }
                }
            }
        }

        String[][] books = {
                {"9780132350884", "Clean Code", "Robert C. Martin", "Software Engineering", "8", "FALSE"},
                {"9780262033848", "Introduction to Algorithms", "Thomas H. Cormen", "Computer Science", "5", "FALSE"},
                {"9780073523323", "Database System Concepts", "Abraham Silberschatz", "Databases", "6", "FALSE"},
                {"9780199535569", "The Oxford English Dictionary", "Oxford University Press", "Reference", "2", "TRUE"},
                {"9780134494166", "The Linux Programming Interface", "Michael Kerrisk", "Operating Systems", "4", "FALSE"},
                {"9780134685991", "Effective Java", "Joshua Bloch", "Software Engineering", "5", "FALSE"},
                {"9781449373320", "Designing Data-Intensive Applications", "Martin Kleppmann", "Databases", "4", "FALSE"},
                {"9780596007126", "Head First Design Patterns", "Eric Freeman and Elisabeth Robson", "Software Engineering", "3", "FALSE"},
                {"9780135957059", "The Pragmatic Programmer", "David Thomas and Andrew Hunt", "Software Engineering", "4", "FALSE"},
                {"9781593279288", "Python Crash Course", "Eric Matthes", "Programming", "5", "FALSE"}
        };
        try (PreparedStatement find = connection.prepareStatement("SELECT id FROM books WHERE isbn=?");
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO books(isbn,title,author,category,total_copies,available_copies,reference_only) VALUES(?,?,?,?,?,?,?)")) {
            for (String[] book : books) {
                find.setString(1, book[0]);
                try (ResultSet result = find.executeQuery()) {
                    if (!result.next()) {
                        insert.setString(1, book[0]);
                        insert.setString(2, book[1]);
                        insert.setString(3, book[2]);
                        insert.setString(4, book[3]);
                        int copies = Integer.parseInt(book[4]);
                        insert.setInt(5, copies);
                        insert.setInt(6, copies);
                        insert.setBoolean(7, Boolean.parseBoolean(book[5]));
                        insert.executeUpdate();
                    }
                }
            }
        }

        String[][] settings = {{"loan_days", "14"}, {"fine_per_day", "5.00"}, {"borrowing_limit", "5"}};
        for (String[] setting : settings) {
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT setting_key FROM library_settings WHERE setting_key=?")) {
                select.setString(1, setting[0]);
                try (ResultSet result = select.executeQuery()) {
                    if (!result.next()) {
                        try (PreparedStatement insert = connection.prepareStatement(
                                "INSERT INTO library_settings(setting_key,setting_value) VALUES(?,?)")) {
                            insert.setString(1, setting[0]);
                            insert.setString(2, setting[1]);
                            insert.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    private static void ensureUser(Connection connection, String email, String role, String name) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT id FROM users WHERE email=?")) {
            select.setString(1, email);
            try (ResultSet result = select.executeQuery()) {
                if (result.next()) return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO users(email,password_hash,role,full_name,enabled) VALUES(?,NULL,?,?,TRUE)")) {
            insert.setString(1, email);
            insert.setString(2, role);
            insert.setString(3, name);
            insert.executeUpdate();
        }
    }

    private static long userId(Connection connection, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM users WHERE email=?")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Local library seed user is missing.");
                return result.getLong(1);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String environmentValue(String name) {
        String value = System.getenv(name);
        return value == null ? null : value.trim();
    }
}
