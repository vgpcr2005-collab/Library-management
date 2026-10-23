package edu.alliance.library.dao;

import edu.alliance.library.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportDao {
    public List<Map<String, Object>> get(String type, String studentId) throws SQLException {
        String sql = switch (type) {
            case "issued" -> "SELECT b.title,b.isbn,s.student_id,u.full_name,i.issue_date,i.due_date FROM book_issues i JOIN books b ON b.id=i.book_id JOIN students s ON s.id=i.student_id JOIN users u ON u.id=s.user_id WHERE i.returned=FALSE ORDER BY i.due_date";
            case "overdue" -> "SELECT b.title,b.isbn,s.student_id,u.full_name,i.issue_date,i.due_date FROM book_issues i JOIN books b ON b.id=i.book_id JOIN students s ON s.id=i.student_id JOIN users u ON u.id=s.user_id WHERE i.returned=FALSE AND i.due_date<CURRENT_DATE ORDER BY i.due_date";
            case "popular" -> "SELECT b.title,b.author,b.category,COUNT(i.id) total_borrows FROM books b LEFT JOIN book_issues i ON i.book_id=b.id GROUP BY b.id ORDER BY total_borrows DESC LIMIT 50";
            case "available" -> "SELECT title,author,isbn,category,available_copies,total_copies FROM books WHERE available_copies>0 ORDER BY title";
            case "history" -> "SELECT b.title,b.author,i.issue_date,i.due_date,i.returned,r.return_date,r.fine_amount FROM book_issues i JOIN books b ON b.id=i.book_id JOIN students s ON s.id=i.student_id LEFT JOIN book_returns r ON r.issue_id=i.id WHERE s.student_id=? ORDER BY i.issue_date DESC LIMIT 200";
            case "monthly" -> "SELECT YEAR(issue_date) year_key,MONTH(issue_date) month_number,COUNT(*) issued FROM book_issues GROUP BY YEAR(issue_date),MONTH(issue_date) ORDER BY year_key DESC,month_number DESC LIMIT 24";
            default -> throw new IllegalArgumentException("Choose a valid report.");
        };
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            if ("history".equals(type)) p.setString(1, studentId);
            try (ResultSet r = p.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (r.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    if ("monthly".equals(type)) {
                        row.put("month_key", YearMonth.of(r.getInt("year_key"), r.getInt("month_number")).toString());
                        row.put("issued", r.getInt("issued"));
                    } else {
                        int columns = r.getMetaData().getColumnCount();
                        for (int i = 1; i <= columns; i++) row.put(r.getMetaData().getColumnLabel(i), r.getObject(i));
                        if ("overdue".equals(type)) {
                            LocalDate dueDate = r.getDate("due_date").toLocalDate();
                            row.put("overdue_days", Math.max(0, ChronoUnit.DAYS.between(dueDate, LocalDate.now())));
                        }
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }
}
