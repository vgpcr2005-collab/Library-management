package edu.alliance.library;

import edu.alliance.library.dao.BookDao;
import edu.alliance.library.dao.DashboardDao;
import edu.alliance.library.dao.IssueDao;
import edu.alliance.library.dao.LibraryOperatorDao;
import edu.alliance.library.dao.ReportDao;
import edu.alliance.library.dao.StudentDao;
import edu.alliance.library.util.Database;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmbeddedDatabaseTest {
    private static Path databaseDirectory;

    @BeforeAll
    static void useIsolatedDatabase() throws IOException {
        databaseDirectory = Files.createTempDirectory("alliance-library-test-");
        System.setProperty("alliance.library.database.path", databaseDirectory.resolve("library").toString());
    }

    @AfterAll
    static void removeIsolatedDatabase() throws SQLException, IOException {
        try (Connection connection = Database.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
        System.clearProperty("alliance.library.database.path");
        try (var paths = Files.walk(databaseDirectory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    @Test
    void initializesLocalDataAndPersistsIssueAndReturnTransactions() throws SQLException {
        BookDao books = new BookDao();
        StudentDao students = new StudentDao();
        IssueDao issues = new IssueDao();
        assertEquals(5, books.search("", "", "").size());
        assertEquals(2, students.search("").size());

        long bookId = findId("SELECT id FROM books WHERE isbn=?", "9780132350884");
        long studentRecordId = findId("SELECT id FROM students WHERE student_id=?", "AU-DEMO-1001");
        long operatorId = new LibraryOperatorDao().operatorId();
        int initialAvailable = books.find(bookId).getAvailableCopies();

        issues.issue(bookId, studentRecordId, operatorId);
        assertEquals(initialAvailable - 1, books.find(bookId).getAvailableCopies());
        assertEquals(1, issues.activeIssues("").size());
        assertEquals(1, ((Number) new DashboardDao().statistics().get("issued")).intValue());

        long issueId = findId("SELECT id FROM book_issues WHERE student_id=? ORDER BY id DESC FETCH FIRST 1 ROW ONLY",
                Long.toString(studentRecordId));
        assertEquals(0, issues.returnBook(issueId, operatorId).compareTo(BigDecimal.ZERO));
        assertEquals(initialAvailable, books.find(bookId).getAvailableCopies());
        assertThrows(IllegalArgumentException.class, () -> issues.returnBook(issueId, operatorId));
        assertEquals(0, issues.activeIssues("").size());
        assertEquals(0, ((Number) new DashboardDao().statistics().get("issued")).intValue());
        List<Map<String, Object>> monthlyReport = new ReportDao().get("monthly", null);
        assertEquals(1, monthlyReport.size());
        assertEquals(1, ((Number) monthlyReport.get(0).get("issued")).intValue());
    }

    private long findId(String sql, Object value) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Test seed row was not found.");
                return result.getLong(1);
            }
        }
    }
}
