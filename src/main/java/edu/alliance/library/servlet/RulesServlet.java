package edu.alliance.library.servlet;

import edu.alliance.library.dao.SettingsDao;
import edu.alliance.library.util.Database;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/app/rules")
public class RulesServlet extends HttpServlet {
    private final SettingsDao settings = new SettingsDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("settings", settings.all());
            request.getRequestDispatcher("/WEB-INF/views/rules.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Library rules could not be loaded " + Database.safeDiagnostic(e));
            request.setAttribute("error", "Library rules are temporarily unavailable.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }
}
