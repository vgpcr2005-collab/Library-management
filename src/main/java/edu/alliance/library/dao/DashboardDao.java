package edu.alliance.library.dao;

import edu.alliance.library.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardDao {
    public Map<String, Object> statistics() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        String sql = """
                SELECT (SELECT COALESCE(SUM(total_copies),0) FROM books) total_books,
                       (SELECT COALESCE(SUM(available_copies),0) FROM books) available_copies,
                       (SELECT COUNT(*) FROM students) students,
                       (SELECT COUNT(*) FROM book_issues WHERE returned=FALSE) issued,
                       (SELECT COUNT(*) FROM book_issues WHERE returned=FALSE AND due_date<CURRENT_DATE) overdue
                """;
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            r.next();
            stats.put("totalBooks", r.getLong("total_books"));
            stats.put("availableCopies", r.getLong("available_copies"));
            stats.put("students", r.getLong("students"));
            stats.put("issued", r.getLong("issued"));
            stats.put("overdue", r.getLong("overdue"));
        }
        stats.put("categories", categories());
        stats.put("monthly", monthlyActivity());
        stats.put("recent", recentIssues());
        stats.put("popular", popularBooks());
        return stats;
    }

    private List<Map<String, Object>> categories() throws SQLException {
        String sql = "SELECT category,COUNT(*) AS title_count FROM books GROUP BY category ORDER BY title_count DESC LIMIT 6";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (r.next()) list.add(Map.of("label", r.getString(1), "value", r.getInt(2)));
            return list;
        }
    }

    private List<Map<String, Object>> monthlyActivity() throws SQLException {
        YearMonth currentMonth = YearMonth.now();
        YearMonth firstMonth = currentMonth.minusMonths(5);
        LocalDate start = firstMonth.atDay(1);
        LocalDate end = currentMonth.plusMonths(1).atDay(1);
        Map<YearMonth, int[]> activity = new java.util.LinkedHashMap<>();
        for (int offset = 0; offset < 6; offset++) activity.put(firstMonth.plusMonths(offset), new int[2]);

        try (Connection c = Database.getConnection()) {
            collectMonthlyActivity(c, "SELECT issue_date FROM book_issues WHERE issue_date>=? AND issue_date<?",
                    start, end, activity, 0);
            collectMonthlyActivity(c, "SELECT return_date FROM book_returns WHERE return_date>=? AND return_date<?",
                    start, end, activity, 1);
        }
        DateTimeFormatter labelFormat = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<YearMonth, int[]> entry : activity.entrySet()) {
            int[] counts = entry.getValue();
            list.add(Map.of("label", entry.getKey().format(labelFormat), "issues", counts[0], "returns", counts[1]));
        }
        return list;
    }

    private void collectMonthlyActivity(Connection connection, String sql, LocalDate start, LocalDate end,
                                        Map<YearMonth, int[]> activity, int countIndex) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, java.sql.Date.valueOf(start));
            statement.setDate(2, java.sql.Date.valueOf(end));
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    YearMonth month = YearMonth.from(results.getDate(1).toLocalDate());
                    int[] counts = activity.get(month);
                    if (counts != null) counts[countIndex]++;
                }
            }
        }
    }

    private List<Map<String, Object>> recentIssues() throws SQLException {
        String sql = """
                SELECT b.title,u.full_name,s.student_id,i.issue_date,i.due_date,i.returned
                FROM book_issues i JOIN books b ON b.id=i.book_id
                JOIN students s ON s.id=i.student_id JOIN users u ON u.id=s.user_id
                ORDER BY i.id DESC LIMIT 7
                """;
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (r.next()) list.add(Map.of("title", r.getString("title"), "student", r.getString("full_name"),
                    "studentId", r.getString("student_id"), "issueDate", r.getDate("issue_date"),
                    "dueDate", r.getDate("due_date"), "returned", r.getBoolean("returned")));
            return list;
        }
    }

    private List<Map<String, Object>> popularBooks() throws SQLException {
        String sql = """
                SELECT b.title,b.author,COUNT(i.id) borrows FROM books b
                LEFT JOIN book_issues i ON i.book_id=b.id GROUP BY b.id ORDER BY borrows DESC,b.title LIMIT 5
                """;
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet r = p.executeQuery()) {
            List<Map<String, Object>> list = new ArrayList<>();
            while (r.next()) list.add(Map.of("title", r.getString("title"), "author", r.getString("author"),
                    "borrows", r.getInt("borrows")));
            return list;
        }
    }
}
