package edu.alliance.library.servlet;

import edu.alliance.library.dao.ReportDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@WebServlet("/app/reports")
public class ReportsServlet extends HttpServlet {
    private final ReportDao reports = new ReportDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String type = request.getParameter("type");
        if (type == null || type.isBlank()) type = "issued";
        try {
            List<Map<String, Object>> rows = reports.get(type, request.getParameter("studentId"));
            request.setAttribute("type", type);
            request.setAttribute("rows", rows);
            if ("csv".equals(request.getParameter("format"))) {
                exportCsv(response, type, rows);
                return;
            }
            request.getRequestDispatcher("/WEB-INF/views/reports.jsp").forward(request, response);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Report query failed " + edu.alliance.library.util.Database.safeDiagnostic(e));
            request.setAttribute("error", "The requested report is temporarily unavailable.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }

    private void exportCsv(HttpServletResponse response, String type, List<Map<String, Object>> rows) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + type + "-report.csv\"");
        var writer = response.getWriter();
        if (rows.isEmpty()) return;
        writer.println(String.join(",", rows.get(0).keySet().stream().map(this::csv).toList()));
        for (Map<String, Object> row : rows) {
            writer.println(String.join(",", row.values().stream().map(value -> csv(value == null ? "" : value.toString())).toList()));
        }
    }

    private String csv(String value) {
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
