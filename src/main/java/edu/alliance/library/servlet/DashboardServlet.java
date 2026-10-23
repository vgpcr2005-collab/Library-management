package edu.alliance.library.servlet;

import edu.alliance.library.dao.DashboardDao;
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
            getServletContext().log("Dashboard data could not be loaded", e);
            request.setAttribute("error", dashboardError(e));
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }

    private String dashboardError(SQLException exception) {
        String sqlState = exception.getSQLState();
        if (sqlState != null && sqlState.startsWith("08")) {
            return "The local library database could not be opened. Check that your Windows account can write to its .alliance-smart-library folder, then restart Tomcat.";
        }
        return "The local library database could not be initialized. Check the Tomcat log for details, then restart the app.";
    }
}
