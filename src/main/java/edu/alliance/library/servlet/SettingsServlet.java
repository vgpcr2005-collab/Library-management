package edu.alliance.library.servlet;

import edu.alliance.library.dao.SettingsDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

@WebServlet("/app/settings")
public class SettingsServlet extends HttpServlet {
    private final SettingsDao settings = new SettingsDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        load(request, response);
    }

    private void load(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            request.setAttribute("settings", settings.all());
            request.getRequestDispatcher("/WEB-INF/views/settings.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Settings could not be loaded", e);
            request.setAttribute("error", "Library settings are temporarily unavailable.");
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            int loanDays = Integer.parseInt(request.getParameter("loanDays"));
            int borrowingLimit = Integer.parseInt(request.getParameter("borrowingLimit"));
            BigDecimal fine = new BigDecimal(request.getParameter("finePerDay"));
            if (loanDays < 1 || loanDays > 365 || borrowingLimit < 1 || borrowingLimit > 100
                    || fine.signum() < 0 || fine.compareTo(new BigDecimal("10000")) > 0)
                throw new IllegalArgumentException("Use a loan period of 1–365 days, limit of 1–100, and a non-negative fine rate.");
            settings.update(Integer.toString(loanDays), fine.toPlainString(), Integer.toString(borrowingLimit));
            request.getSession().setAttribute("success", "Library settings updated.");
            response.sendRedirect(request.getContextPath() + "/app/settings");
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
            load(request, response);
        } catch (SQLException e) {
            getServletContext().log("Settings update failed", e);
            request.setAttribute("error", "Settings could not be saved.");
            load(request, response);
        }
    }
}
