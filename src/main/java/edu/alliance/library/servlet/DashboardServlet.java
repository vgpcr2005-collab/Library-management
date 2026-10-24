package edu.alliance.library.servlet;

import edu.alliance.library.dao.DashboardDao;
import edu.alliance.library.util.Database;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/app/dashboard")
public class DashboardServlet extends HttpServlet {
    private final DashboardDao dashboard = new DashboardDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            request.setAttribute("stats", dashboard.statistics());
            request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Dashboard data could not be loaded " + Database.safeDiagnostic(e));
            request.setAttribute("error", Database.failureMessage(e));
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }
}
